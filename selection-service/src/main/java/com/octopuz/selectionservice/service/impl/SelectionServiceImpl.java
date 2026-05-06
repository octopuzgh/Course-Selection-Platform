package com.octopuz.selectionservice.service.impl;

import com.octopuz.selectionservice.annotation.LogSelection;
import com.octopuz.selectionservice.client.BasicServiceClient;
import com.octopuz.selectionservice.dto.RankingItem;
import com.octopuz.selectionservice.dto.SelectionRequest;
import com.octopuz.selectionservice.dto.SelectionResponse;
import com.octopuz.selectionservice.producer.SelectionMessageProducer;
import com.octopuz.selectionservice.service.interf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

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

    private static final String LOCK_KEY_PREFIX = "lock:selection:";

    @LogSelection(action = "SELECT")
    @Override
    public SelectionResponse selectCourse(SelectionRequest request) {
        String studentNo = request.getStudentNo();
        String courseNo = request.getCourseNo();

        if (!validateStudentAndCourse(studentNo, courseNo)) {
            return SelectionResponse.fail("学生或课程不存在");
        }

        if (selectedRecordService.isSelected(studentNo, courseNo)) {
            return SelectionResponse.fail("您已选该课程");
        }

        String lockKey = LOCK_KEY_PREFIX + courseNo;
        try {
            if (!lockService.tryLock(lockKey, 5, 10, TimeUnit.SECONDS)) {
                return SelectionResponse.fail("系统繁忙，请稍后重试");
            }

            if (!stockService.checkStock(courseNo)) {
                return SelectionResponse.fail("课程已选满");
            }

            if (!stockService.deductStock(courseNo)) {
                return SelectionResponse.fail("课程已选满");
            }

            selectedRecordService.markSelected(studentNo, courseNo, 1, TimeUnit.HOURS);
            rankingService.updateRanking(courseNo);
            messageProducer.sendSelectionMessage(studentNo, courseNo);

            log.info("学生{}成功选课{}", studentNo, courseNo);
            return SelectionResponse.success(generateSelectionId());

        } finally {
            lockService.unlock(lockKey);
        }
    }

    @LogSelection(action = "DROP")
    @Override
    public SelectionResponse dropCourse(SelectionRequest request) {
        String studentNo = request.getStudentNo();
        String courseNo = request.getCourseNo();

        if (!validateStudentAndCourse(studentNo, courseNo)) {
            return SelectionResponse.fail("学生或课程不存在");
        }

        if (!selectedRecordService.isSelected(studentNo, courseNo)) {
            return SelectionResponse.fail("您未选该课程");
        }

        String lockKey = LOCK_KEY_PREFIX + courseNo;
        try {
            if (!lockService.tryLock(lockKey, 5, 10, TimeUnit.SECONDS)) {
                return SelectionResponse.fail("系统繁忙，请稍后重试");
            }

            stockService.restoreStock(courseNo);
            selectedRecordService.unmarkSelected(studentNo, courseNo);
            rankingService.restoreRanking(courseNo);
            messageProducer.sendDropMessage(studentNo, courseNo);

            log.info("学生{}成功退课{}", studentNo, courseNo);
            return SelectionResponse.success(generateSelectionId());

        } finally {
            lockService.unlock(lockKey);
        }
    }

    @Override
    public List<RankingItem> getAllCourses(Integer page, Integer size) {
        return rankingService.getAllCourses(page, size);
    }

    private boolean validateStudentAndCourse(String studentNo, String courseNo) {
        String student = basicServiceClient.getStudent(studentNo);
        String course = basicServiceClient.getCourse(courseNo);
        return student != null && course != null;
    }

    private Long generateSelectionId() {
        return System.currentTimeMillis() + (long) (Math.random() * 1000);
    }
}
