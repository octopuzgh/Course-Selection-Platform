package com.octopuz.selectionservice.controller;

import com.octopuz.selectionservice.dto.RankingItem;
import com.octopuz.selectionservice.dto.SelectionRequest;
import com.octopuz.selectionservice.dto.SelectionResponse;
import com.octopuz.selectionservice.service.interf.SelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/ranking")
    public ResponseEntity<List<RankingItem>> getAllCourses(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        List<RankingItem> ranking = selectionService.getAllCourses(page, size);
        return ResponseEntity.ok(ranking);
    }
}
