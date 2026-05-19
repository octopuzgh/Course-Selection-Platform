package com.octopuz.selectionservice.consumer;

import com.alibaba.fastjson.JSON;
import com.octopuz.selectionservice.client.BasicServiceClient;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SelectionConsumer {

    @Autowired
    private BasicServiceClient basicServiceClient;

    @KafkaListener(topics = "selection-topic", groupId = "${KAFKA_GROUP_ID}")
    public void consume(@Payload String message, Acknowledgment ack) {
        log.debug("收到选课消息: {}", message);
        try {
            SelectionMessage msg = JSON.parseObject(message, SelectionMessage.class);

            if ("DROP".equals(msg.getType())) {
                boolean success = basicServiceClient.dropSelection(msg.getStudentNo(), msg.getCourseNo());
                if (success) {
                    log.info("退课成功: 学生{} 课程{}", msg.getStudentNo(), msg.getCourseNo());
                } else {
                    log.warn("退课失败: 学生{} 课程{}", msg.getStudentNo(), msg.getCourseNo());
                }
            } else {
                boolean success = basicServiceClient.submitSelection(msg.getStudentNo(), msg.getCourseNo());
                if (success) {
                    log.info("选课成功: 学生{} 课程{}", msg.getStudentNo(), msg.getCourseNo());
                } else {
                    log.warn("选课失败: 学生{} 课程{}", msg.getStudentNo(), msg.getCourseNo());
                }
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("处理选课消息失败: {}", message, e);
        }
    }

    @Data
    private static class SelectionMessage {
        private String studentNo;
        private String courseNo;
        private String type;
    }
}
