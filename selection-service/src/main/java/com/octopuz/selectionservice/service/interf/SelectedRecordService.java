package com.octopuz.selectionservice.service.interf;

import java.util.concurrent.TimeUnit;

/**
 * 已选记录服务接口
 */
public interface SelectedRecordService {

    /**
     * 检查是否已选该课程（先查Redis，再查数据库）
     * @param studentNo 学号
     * @param courseNo 课程号
     * @return true表示已选
     */
    boolean isSelected(String studentNo, String courseNo);

    /**
     * 标记已选
     * @param studentNo 学号
     * @param courseNo 课程号
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     */
    void markSelected(String studentNo, String courseNo, long expireTime, TimeUnit timeUnit);

    /**
     * 取消已选标记
     * @param studentNo 学号
     * @param courseNo 课程号
     */
    void unmarkSelected(String studentNo, String courseNo);
}
