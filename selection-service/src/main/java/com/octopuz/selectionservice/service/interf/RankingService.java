package com.octopuz.selectionservice.service.interf;

import com.octopuz.selectionservice.dto.RankingItem;

import java.util.List;

/**
 * 排行榜服务接口
 */
public interface RankingService {

    /**
     * 更新排行榜（选课时调用，剩余数量-1）
     * @param courseNo 课程号
     */
    void updateRanking(String courseNo);

    /**
     * 恢复排行榜（退课时调用，剩余数量+1）
     * @param courseNo 课程号
     */
    void restoreRanking(String courseNo);

    /**
     * 初始化课程排行榜
     * @param courseNo 课程号
     * @param remaining 剩余数量
     */
    void initCourseRanking(String courseNo, Integer remaining);

    /**
     * 分页获取所有课程排行（从剩余多到剩余少）
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 排行榜列表
     */
    List<RankingItem> getAllCourses(Integer page, Integer size);

    /**
     * 获取课程当前排名
     * @param courseNo 课程号
     * @return 排名（从1开始），不存在返回-1
     */
    Long getRank(String courseNo);

    /**
     * 获取课程剩余数量
     * @param courseNo 课程号
     * @return 剩余数量
     */
    Long getScore(String courseNo);
}
