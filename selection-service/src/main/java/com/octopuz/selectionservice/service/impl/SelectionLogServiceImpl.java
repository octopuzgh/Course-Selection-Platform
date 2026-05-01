package com.octopuz.selectionservice.service.impl;

import com.octopuz.selectionservice.entity.SelectionLog;
import com.octopuz.selectionservice.mapper.SelectionLogMapper;
import com.octopuz.selectionservice.service.interf.SelectionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SelectionLogServiceImpl implements SelectionLogService {

    @Autowired
    private SelectionLogMapper selectionLogMapper;

    @Override
    public void logSelection(String studentNo, String courseNo, String operator, String action) {
        SelectionLog logEntry = SelectionLog.builder()
                .studentNo(studentNo)
                .courseNo(courseNo)
                .operator(operator)
                .action(action)
                .build();

        selectionLogMapper.insert(logEntry);
        log.debug("选课日志记录: studentNo={}, courseNo={}, operator={}, action={}",
                studentNo, courseNo, operator, action);
    }
}
