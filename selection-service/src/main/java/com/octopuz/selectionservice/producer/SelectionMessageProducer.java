package com.octopuz.selectionservice.producer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.octopuz.selectionservice.client.BasicServiceClient;
import com.octopuz.selectionservice.entity.SelectionLog;
import com.octopuz.selectionservice.entity.SelectionRecord;
import com.octopuz.selectionservice.mapper.SelectionLogMapper;
import com.octopuz.selectionservice.mapper.SelectionRecordMapper;
import com.octopuz.selectionservice.service.interf.StockService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SelectionMessageProducer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private SelectionRecordMapper selectionRecordMapper;
    @Autowired
    private SelectionLogMapper selectionLogMapper;
    @Autowired
    private StockService stockService;
    @Autowired
    private BasicServiceClient basicServiceClient;

    @Value("${KAFKA_TOPIC}")
    private String selectionTopic;

    public void sendSelectionMessage(String studentNo, String courseNo) {
        sendMessage(studentNo, courseNo, "SELECT");
    }

    public void sendDropMessage(String studentNo, String courseNo) {
        sendMessage(studentNo, courseNo, "DROP");
    }

    private void sendMessage(String studentNo, String courseNo, String type) {
        try {
            SelectionMessage message = new SelectionMessage(studentNo, courseNo, type);
            String json = JSON.toJSONString(message);
            kafkaTemplate.send(selectionTopic, json);
            log.debug("发送选课消息到Kafka: {}", json);
        } catch (Exception e) {
            log.error("发送Kafka消息失败（已重试3次），降级同步写库", e);
            fallbackWrite(studentNo, courseNo, type);
        }
    }

    private void fallbackWrite(String studentNo, String courseNo, String type) {
        if ("SELECT".equals(type)) {
            SelectionLog logEntry = SelectionLog.builder()
                    .studentNo(studentNo)
                    .courseNo(courseNo)
                    .operator(studentNo)
                    .action("SELECT")
                    .build();
            selectionLogMapper.insert(logEntry);

            basicServiceClient.submitSelection(studentNo, courseNo);

            log.info("降级选课处理完成：学生{} 课程{}", studentNo, courseNo);
        } else if ("DROP".equals(type)) {
            selectionLogMapper.delete(new QueryWrapper<SelectionLog>()
                    .eq("student_no", studentNo)
                    .eq("course_no", courseNo)
                    .eq("action", "DROP"));

            stockService.restoreStock(courseNo);
            basicServiceClient.dropSelection(studentNo, courseNo);

            log.info("降级退课处理完成：学生{} 课程{}", studentNo, courseNo);
        }
    }

    @Data
    @AllArgsConstructor
    private static class SelectionMessage {
        private String studentNo;
        private String courseNo;
        private String type;
    }
}
