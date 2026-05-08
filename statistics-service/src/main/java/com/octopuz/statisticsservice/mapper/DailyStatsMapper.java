package com.octopuz.statisticsservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.octopuz.statisticsservice.entity.DailyStats;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DailyStatsMapper extends BaseMapper<DailyStats> {
}