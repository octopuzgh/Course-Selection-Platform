package com.octopuz.selectionservice.service.interf;

import com.octopuz.selectionservice.dto.RankingItem;
import com.octopuz.selectionservice.dto.SelectionRequest;
import com.octopuz.selectionservice.dto.SelectionResponse;

import java.util.List;

public interface SelectionService {
    
    /**
     * 选课
     * @param request 选课请求
     * @return 选课结果
     */
    SelectionResponse selectCourse(SelectionRequest request);

    /**
     * 分页获取所有课程排行
     * @param page 页码
     * @param size 每页数量
     * @return 排行榜列表
     */
    List<RankingItem> getAllCourses(Integer page, Integer size);
}
