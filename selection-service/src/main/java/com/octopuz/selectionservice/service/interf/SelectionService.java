package com.octopuz.selectionservice.service.interf;

import com.octopuz.selectionservice.dto.SelectionRequest;
import com.octopuz.selectionservice.dto.SelectionResponse;

public interface SelectionService {
    
    /**
     * 选课
     * @param request 选课请求
     * @return 选课结果
     */
    SelectionResponse selectCourse(SelectionRequest request);

    /**
     * 退课
     * @param request 退课请求
     * @return 退课结果
     */
    SelectionResponse dropCourse(SelectionRequest request);
}
