package com.simon.basicservice.service.interf;

import com.baomidou.mybatisplus.extension.service.IService;
import com.simon.basicservice.entity.Selection_Record;

public interface Selection_RecordService extends IService<Selection_Record> {

    //检查学生是否已选某课程
    boolean hasSelected(String studentNo, String courseNo);

    //保存选课记录（仅插入，供 Kafka 或其他服务调用）
    void saveSelection(String studentNo, String courseNo);

    //完整选课流程：验证 -> 扣库存 -> 插入记录（8080 自己使用）
    String fullSelectCourse(String studentNo, String courseNo);
}
