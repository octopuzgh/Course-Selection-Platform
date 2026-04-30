package com.octopuz.selectionservice.service.impl;

import com.octopuz.selectionservice.client.BasicServiceClient;
import com.octopuz.selectionservice.service.interf.SelectedRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SelectedRecordServiceImpl implements SelectedRecordService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private BasicServiceClient basicServiceClient;
    private static final String SELECTED_KEY_PREFIX = "selected:";

    @Override
    public boolean isSelected(String studentNo, String courseNo) {
        String key = SELECTED_KEY_PREFIX + studentNo + ":" + courseNo;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return true;
        }
        return basicServiceClient.hasSelectedCourse(studentNo, courseNo);
    }

    @Override
    public void markSelected(String studentNo, String courseNo, long expireTime, TimeUnit timeUnit) {
        String key = SELECTED_KEY_PREFIX + studentNo + ":" + courseNo;
        redisTemplate.opsForValue().set(key, "1", expireTime, timeUnit);
        log.debug("学生{}课程{}标记为已选，过期时间: {} {}", studentNo, courseNo, expireTime, timeUnit);
    }

    @Override
    public void unmarkSelected(String studentNo, String courseNo) {
        String key = SELECTED_KEY_PREFIX + studentNo + ":" + courseNo;
        redisTemplate.delete(key);
        log.debug("学生{}课程{}取消已选标记", studentNo, courseNo);
    }
}
