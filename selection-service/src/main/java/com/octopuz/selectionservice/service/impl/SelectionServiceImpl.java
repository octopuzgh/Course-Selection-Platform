package com.octopuz.selectionservice.service.impl;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.octopuz.selectionservice.annotation.LogSelection;
import com.octopuz.selectionservice.client.BasicServiceClient;
import com.octopuz.selectionservice.dto.SelectionRequest;
import com.octopuz.selectionservice.dto.SelectionResponse;
import com.octopuz.selectionservice.exception.BusinessException;
import com.octopuz.selectionservice.producer.SelectionMessageProducer;
import com.octopuz.selectionservice.service.interf.LockService;
import com.octopuz.selectionservice.service.interf.RankingService;
import com.octopuz.selectionservice.service.interf.SelectedRecordService;
import com.octopuz.selectionservice.service.interf.SelectionService;
import com.octopuz.selectionservice.service.interf.StockService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SelectionServiceImpl implements SelectionService {

    @Autowired
    private LockService lockService;
    @Autowired
    private StockService stockService;
    @Autowired
    private SelectedRecordService selectedRecordService;
    @Autowired
    private RankingService rankingService;
    @Autowired
    private SelectionMessageProducer messageProducer;
    @Autowired
    private BasicServiceClient basicServiceClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String LOCK_KEY_PREFIX = "lock:selection:";

    @LogSelection(action = "SELECT")
    @Override
    public SelectionResponse selectCourse(SelectionRequest request) {
        String studentNo = request.getStudentNo();
        String courseNo = request.getCourseNo();

        log.info("[SELECT] 学生{}开始选课{}, Thread={}", studentNo, courseNo, Thread.currentThread().getId());

        if (!validateStudentAndCourse(studentNo, courseNo)) {
            throw new BusinessException("学生或课程不存在");
        }

        String lockKey = LOCK_KEY_PREFIX + courseNo;
        log.info("[SELECT] 学生{}尝试获取锁{}, Thread={}", studentNo, lockKey, Thread.currentThread().getId());
        boolean lockAcquired = false;
        try {
            lockAcquired = lockService.tryLock(lockKey, 5, 10, TimeUnit.SECONDS);
            log.info("[SELECT] 学生{}获取锁结果: {}, Thread={}", studentNo, lockAcquired, Thread.currentThread().getId());
            if (!lockAcquired) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }

            if (selectedRecordService.isSelected(studentNo, courseNo)) {
                log.info("[SELECT] 学生{}已选过课程{}, Thread={}", studentNo, courseNo, Thread.currentThread().getId());
                throw new BusinessException("您已选该课程");
            }

            if (!stockService.checkStock(courseNo)) {
                throw new BusinessException("课程已选满");
            }

            if (!stockService.deductStock(courseNo)) {
                throw new BusinessException("课程已选满");
            }

            selectedRecordService.markSelected(studentNo, courseNo, 1, TimeUnit.HOURS);
            rankingService.updateRanking(courseNo);
            messageProducer.sendSelectionMessage(studentNo, courseNo);

            log.info("学生{}成功选课{}", studentNo, courseNo);
            return SelectionResponse.success(generateSelectionId());

        } finally {
            if (lockAcquired) {
                lockService.unlock(lockKey);
                log.info("[SELECT] 学生{}释放锁{}, Thread={}", studentNo, lockKey, Thread.currentThread().getId());
            }
        }
    }

    @LogSelection(action = "DROP")
    @Override
    public SelectionResponse dropCourse(SelectionRequest request) {
        String studentNo = request.getStudentNo();
        String courseNo = request.getCourseNo();

        if (!validateStudentAndCourse(studentNo, courseNo)) {
            throw new BusinessException("学生或课程不存在");
        }

        if (!selectedRecordService.isSelected(studentNo, courseNo)) {
            throw new BusinessException("您未选该课程");
        }

        String lockKey = LOCK_KEY_PREFIX + courseNo;
        boolean lockAcquired = false;
        try {
            lockAcquired = lockService.tryLock(lockKey, 5, 10, TimeUnit.SECONDS);
            if (!lockAcquired) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }

            stockService.restoreStock(courseNo);
            selectedRecordService.unmarkSelected(studentNo, courseNo);
            rankingService.restoreRanking(courseNo);
            messageProducer.sendDropMessage(studentNo, courseNo);

            log.info("学生{}成功退课{}", studentNo, courseNo);
            return SelectionResponse.success(generateSelectionId());

        } finally {
            if (lockAcquired) {
                lockService.unlock(lockKey);
            }
        }
    }

    private boolean validateStudentAndCourse(String studentNo, String courseNo) {
        String student = basicServiceClient.getStudent(studentNo);
        String course = basicServiceClient.getCourse(courseNo);
        return student != null && course != null;
    }

    private Long generateSelectionId() {
        return redisTemplate.opsForValue().increment("global:selection:id");
    }
}
