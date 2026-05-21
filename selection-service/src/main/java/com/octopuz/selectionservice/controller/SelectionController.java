package com.octopuz.selectionservice.controller;

import com.octopuz.selectionservice.dto.AdminSelectionRequest;
import com.octopuz.selectionservice.dto.SelectionRequest;
import com.octopuz.selectionservice.dto.SelectionResponse;
import com.octopuz.selectionservice.service.interf.SelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/selection")
@RequiredArgsConstructor
public class SelectionController {

    @Autowired
    private SelectionService selectionService;

    @PostMapping("/select")
    public ResponseEntity<SelectionResponse> selectCourse(@RequestBody SelectionRequest request) {
        log.info("收到选课请求: studentNo={}, courseNo={}", request.getStudentNo(), request.getCourseNo());
        SelectionResponse response = selectionService.selectCourse(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/drop")
    public ResponseEntity<SelectionResponse> dropCourse(@RequestBody SelectionRequest request) {
        log.info("收到退课请求: studentNo={}, courseNo={}", request.getStudentNo(), request.getCourseNo());
        SelectionResponse response = selectionService.dropCourse(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/select")
    public ResponseEntity<SelectionResponse> adminSelectCourse(@RequestBody AdminSelectionRequest request) {
        log.info("[ADMIN] 帮学生选课: studentNo={}, courseNo={}", request.getStudentNo(), request.getCourseNo());
        SelectionRequest selectionRequest = new SelectionRequest();
        selectionRequest.setStudentNo(request.getStudentNo());
        selectionRequest.setCourseNo(request.getCourseNo());
        SelectionResponse response = selectionService.selectCourse(selectionRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/drop")
    public ResponseEntity<SelectionResponse> adminDropCourse(@RequestBody AdminSelectionRequest request) {
        log.info("[ADMIN] 帮学生退课: studentNo={}, courseNo={}", request.getStudentNo(), request.getCourseNo());
        SelectionRequest selectionRequest = new SelectionRequest();
        selectionRequest.setStudentNo(request.getStudentNo());
        selectionRequest.setCourseNo(request.getCourseNo());
        SelectionResponse response = selectionService.dropCourse(selectionRequest);
        return ResponseEntity.ok(response);
    }
}