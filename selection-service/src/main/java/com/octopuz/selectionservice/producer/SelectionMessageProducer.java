package com.octopuz.selectionservice.producer;

import com.alibaba.fastjson.JSON;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SelectionMessageProducer {


    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    private static final String SELECTION_TOPIC = "selection_topic";

    public void sendSelectionMessage(String studentNo, String courseNo) {
        try {
            SelectionMessage message = new SelectionMessage(studentNo, courseNo);
            String json = JSON.toJSONString(message);
            kafkaTemplate.send(SELECTION_TOPIC, json);
            log.debug("发送选课消息到Kafka: {}", json);
        } catch (Exception e) {
            log.error("发送Kafka消息失败", e);
        }
    }

    @Data
    @AllArgsConstructor
    private static class SelectionMessage {
        private String studentNo;
        private String courseNo;
    }
}
