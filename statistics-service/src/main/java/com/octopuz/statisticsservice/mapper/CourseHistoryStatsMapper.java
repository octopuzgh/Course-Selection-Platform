package com.octopuz.statisticsservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.octopuz.statisticsservice.entity.CourseHistoryStats;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CourseHistoryStatsMapper extends BaseMapper<CourseHistoryStats> {

    @Update("TRUNCATE TABLE course_history_stats")
    void truncate();

    @Insert("<script>" +
            "INSERT INTO course_history_stats (course_no, total_selected, rank) " +
            "VALUES " +
            "<foreach collection='list' item='item' separator=','> " +
            "(#{item.courseNo}, #{item.totalSelected}, #{item.rank}) " +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("list") List<CourseHistoryStats> list);
}