package com.octopuz.selectionservice.consumer;

import com.alibaba.fastjson.JSON;
import com.octopuz.selectionservice.client.BasicServiceClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SelectionConsumer {

    private final BasicServiceClient basicServiceClient;

    public SelectionConsumer(BasicServiceClient basicServiceClient) {
        this.basicServiceClient = basicServiceClient;
    }

    @KafkaListener(topics = "selection_topic", groupId = "selection-consumer-group")
    public void consume(String message) {
        log.debug("收到选课消息: {}", message);
        try {
            SelectionMessage msg = JSON.parseObject(message, SelectionMessage.class);

            boolean success = basicServiceClient.saveSelectionRecord(msg.getStudentNo(), msg.getCourseNo());

            if (success) {
                log.info("选课记录写入成功: 学生{} 课程{}", msg.getStudentNo(), msg.getCourseNo());
            } else {
                log.error("选课记录写入失败: 学生{} 课程{}", msg.getStudentNo(), msg.getCourseNo());
            }
        } catch (Exception e) {
            log.error("处理选课消息失败", e);
        }
    }
    @Data
    private static class SelectionMessage {
        private String studentNo;
        private String courseNo;
    }
}
