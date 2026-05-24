package com.simon.basicservice.controller;

import com.simon.basicservice.annotation.RequireRole;
import com.simon.basicservice.common.ErrorCode;
import com.simon.basicservice.common.Result;
import com.simon.basicservice.dto.CourseCreateRequest;
import com.simon.basicservice.dto.CourseQueryRequest;
import com.simon.basicservice.entity.Course;
import com.simon.basicservice.service.interf.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "课程管理", description = "课程的查询和管理接口")
@RestController
@RequestMapping("/courses")
public class CourseController {
    @Autowired
    private CourseService courseService;

    @Operation(summary = "创建课程", description = "新增一个课程信息")
    @RequireRole(RequireRole.ADMIN)
    @PostMapping
    public Result<Course> createCourse(@Valid @RequestBody CourseCreateRequest request) {
        Course existingCourse = courseService.getByCourseNo(request.getCourseNo());
        if (existingCourse != null) {
            return Result.error(ErrorCode.CONFLICT);
        }

        Course course = new Course();
        course.setCourseNo(request.getCourseNo());
        course.setCourseName(request.getCourseName());
        course.setTeacher(request.getTeacher());
        course.setCredit(request.getCredit());
        course.setTotalCapacity(request.getTotalCapacity());
        course.setRemaining(request.getRemaining());
        course.setSelectedCount(request.getSelectedCount());

        boolean success = courseService.save(course);
        if (success) {
            return Result.success(course);
        }
        return Result.error(ErrorCode.SERVER_ERROR);
    }

    @Operation(summary = "删除课程", description = "根据课程号删除课程信息")
    @RequireRole(RequireRole.ADMIN)
    @DeleteMapping("/{courseNo}")
    public Result<Void> deleteCourse(@PathVariable String courseNo) {
        // 校验课程号格式：必须由大写字母和数字组成，首字母大写
        if (courseNo == null || courseNo.trim().isEmpty()) {
            return Result.error(ErrorCode.BAD_REQUEST, "课程号不能为空");
        }
        
        if (!courseNo.matches("^[A-Z][A-Z0-9]*$")) {
            return Result.error(ErrorCode.BAD_REQUEST, "课程号必须由大写字母和数字组成，且必须以一个大写字母开头");
        }
        
        Course course = courseService.getByCourseNo(courseNo);
        if (course == null) {
            return Result.error(ErrorCode.COURSE_NOT_FOUND);
        }

        boolean success = courseService.removeById(course.getId());
        if (success) {
            return Result.success(null);
        }
        return Result.error(ErrorCode.SERVER_ERROR);
    }

    @Operation(summary = "查询单个课程", description = "根据课程号查询课程详细信息")
    @GetMapping("/{courseNo}")
    public Result<Course> getCourse(@PathVariable String courseNo) {
        // 校验课程号格式：必须由大写字母和数字组成，首字母大写
        if (courseNo == null || courseNo.trim().isEmpty()) {
            return Result.error(ErrorCode.BAD_REQUEST, "课程号不能为空");
        }
        
        if (!courseNo.matches("^[A-Z][A-Z0-9]*$")) {
            return Result.error(ErrorCode.BAD_REQUEST, "课程号必须由大写字母和数字组成，且必须以一个大写字母开头");
        }
        
        Course course = courseService.getByCourseNo(courseNo);
        if (course == null) {
            return Result.error(ErrorCode.COURSE_NOT_FOUND);
        }
        return Result.success(course);
    }

    @Operation(summary = "获取所有课程列表", description = "查询系统中所有课程的列表")
    @GetMapping
    public Result<List<Course>> getAllCourses() {
        return Result.success(courseService.listAll());
    }

    @Operation(summary = "扣减课程名额", description = "扣减指定课程的剩余名额，用于选课操作")
    @RequireRole(RequireRole.ADMIN)
    @PutMapping("/{courseNo}/decrement")
    public Result<Course> decrement(@PathVariable String courseNo) {
        // 校验课程号格式：必须由大写字母和数字组成，首字母大写
        if (courseNo == null || courseNo.trim().isEmpty()) {
            return Result.error(ErrorCode.BAD_REQUEST, "课程号不能为空");
        }
        
        if (!courseNo.matches("^[A-Z][A-Z0-9]*$")) {
            return Result.error(ErrorCode.BAD_REQUEST, "课程号必须由大写字母和数字组成，且必须以一个大写字母开头");
        }
        
        Course updated = courseService.decrementRemaining(courseNo);
        if (updated == null) {
            return Result.error(ErrorCode.COURSE_STOCK_INSUFFICIENT);
        }
        return Result.success(updated);
    }

    @Operation(summary = "增加课程名额", description = "增加指定课程的剩余名额，用于退课操作（仅管理员）")
    @RequireRole(RequireRole.ADMIN)
    @PutMapping("/{courseNo}/increment")
    public Result<Course> increment(@PathVariable String courseNo) {
        // 校验课程号格式：必须由大写字母和数字组成，首字母大写
        if (courseNo == null || courseNo.trim().isEmpty()) {
            return Result.error(ErrorCode.BAD_REQUEST, "课程号不能为空");
        }
        
        if (!courseNo.matches("^[A-Z][A-Z0-9]*$")) {
            return Result.error(ErrorCode.BAD_REQUEST, "课程号必须由大写字母和数字组成，且必须以一个大写字母开头");
        }
        
        Course updated = courseService.incrementRemaining(courseNo);
        if (updated == null) {
            return Result.error(ErrorCode.COURSE_NOT_FOUND);
        }
        return Result.success(updated);
    }

}
