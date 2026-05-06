package com.octopuz.selectionservice.consumer;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.octopuz.selectionservice.entity.SelectionRecord;
import com.octopuz.selectionservice.mapper.SelectionRecordMapper;
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
    private SelectionRecordMapper selectionRecordMapper;

    @KafkaListener(topics = "selection-topic", groupId = "${KAFKA_GROUP_ID}")
    public void consume(@Payload String message, Acknowledgment ack) {
        log.debug("收到选课消息: {}", message);
        try {
            SelectionMessage msg = JSON.parseObject(message, SelectionMessage.class);

            if ("DROP".equals(msg.getType())) {
                QueryWrapper<SelectionRecord> wrapper = new QueryWrapper<>();
                wrapper.eq("student_no", msg.getStudentNo())
                       .eq("course_no", msg.getCourseNo());
                selectionRecordMapper.delete(wrapper);
                log.info("退课记录删除成功: 学生{} 课程{}", msg.getStudentNo(), msg.getCourseNo());
            } else {
                SelectionRecord record = SelectionRecord.builder()
                        .studentNo(msg.getStudentNo())
                        .courseNo(msg.getCourseNo())
                        .build();
                selectionRecordMapper.insert(record);
                log.info("选课记录写入成功: 学生{} 课程{}", msg.getStudentNo(), msg.getCourseNo());
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
