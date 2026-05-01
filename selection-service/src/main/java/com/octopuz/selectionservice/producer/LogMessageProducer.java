package com.octopuz.selectionservice.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogMessageProducer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    private static final String LOG_TOPIC = "selection-log-topic";

    public void sendLogMessage(String studentNo, String courseNo, String operator, String action) {
        String message = String.format("{\"studentNo\":\"%s\",\"courseNo\":\"%s\",\"operator\":\"%s\",\"action\":\"%s\"}",
                studentNo, courseNo, operator, action);
        kafkaTemplate.send(LOG_TOPIC, message);
        log.debug("发送日志消息: {}", message);
    }
}
