package com.simon.basicservice.controller;

import com.simon.basicservice.common.Result;
import com.simon.basicservice.entity.Course;
import com.simon.basicservice.service.interf.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "课程管理", description = "课程的查询和管理接口")
@RestController
@RequestMapping("/courses")
public class CourseController {
    @Autowired
    private CourseService courseService;

    // 查询单个课程
    @Operation(summary = "查询单个课程", description = "根据课程号查询课程详细信息")
    @GetMapping("/{courseNo}")
    public Result<Course> getCourse(@PathVariable String courseNo) {
        Course course = courseService.getByCourseNo(courseNo);
        if (course == null) {
            return Result.error(404, "课程不存在");
        }
        return Result.success(course);
    }

    // 获取所有课程列表
    @Operation(summary = "获取所有课程列表", description = "查询系统中所有课程的列表")
    @GetMapping
    public Result<List<Course>> getAllCourses() {
        return Result.success(courseService.listAll());
    }

    // 扣减名额（供8081调用）
    @Operation(summary = "扣减课程名额", description = "扣减指定课程的剩余名额，用于选课操作")
    @PutMapping("/{courseNo}/decrement")
    public Result<Course> decrement(@PathVariable String courseNo) {
        Course updated = courseService.decrementRemaining(courseNo);
        if (updated == null) {
            return Result.error(409, "库存不足或并发冲突");
        }
        return Result.success(updated);
    }
}