package com.octopuz.selectionservice.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.octopuz.selectionservice.entity.SelectionLog;
import com.octopuz.selectionservice.mapper.SelectionLogMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogConsumer {

    @Autowired
    private SelectionLogMapper selectionLogMapper;

    @KafkaListener(topics = "selection-log-topic", groupId = "${KAFKA_GROUP_ID}")
    public void consumeLog(@Payload String message, Acknowledgment ack) {
        log.debug("收到日志消息: {}", message);
        try {
            JSONObject json = JSON.parseObject(message);

            SelectionLog logEntry = SelectionLog.builder()
                    .studentNo(json.getString("studentNo"))
                    .courseNo(json.getString("courseNo"))
                    .operator(json.getString("operator"))
                    .action(json.getString("action"))
                    .build();

            selectionLogMapper.insert(logEntry);

            log.info("日志写入成功: studentNo={}, courseNo={}, operator={}, action={}",
                    logEntry.getStudentNo(), logEntry.getCourseNo(),
                    logEntry.getOperator(), logEntry.getAction());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("处理日志消息失败: {}", message, e);
        }
    }
}
