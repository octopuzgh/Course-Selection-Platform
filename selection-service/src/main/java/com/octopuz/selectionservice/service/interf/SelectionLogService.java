package com.octopuz.selectionservice.service.interf;

public interface SelectionLogService {

    /**
     * 记录选课日志
     * @param studentNo 学号（可为空，系统操作时）
     * @param courseNo 课程号
     * @param operator 操作人
     * @param action 操作类型：SELECT-选课，DROP-退课
     */
    void logSelection(String studentNo, String courseNo, String operator, String action);
}
