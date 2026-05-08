package com.simon.basicservice.controller;

import com.simon.basicservice.common.Result;
import com.simon.basicservice.dto.StudentSelection;
import com.simon.basicservice.entity.Course;
import com.simon.basicservice.entity.Selection_Record;
import com.simon.basicservice.entity.Student;
import com.simon.basicservice.service.interf.CourseService;
import com.simon.basicservice.service.interf.Selection_RecordService;
import com.simon.basicservice.service.interf.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "学生管理", description = "学生信息的查询接口")
@RestController
@RequestMapping("/students")
public class StudentController {
    @Autowired
    private StudentService studentService;

    @Autowired
    private Selection_RecordService selectionRecordService;

    @Autowired
    private CourseService courseService;

    //查询学生列表
    @Operation(summary = "获取所有学生列表", description = "查询系统中所有学生的列表")
    @GetMapping
    public Result<List<Student>> getAllStudents() {
        List<Student> students = studentService.list();
        return Result.success(students);
    }

    // 查询学生（path中为学号）
    @Operation(summary = "查询学生信息", description = "根据学号查询学生的详细信息")
    @GetMapping("/{studentNo}")
    public Result<Student> getStudent(@PathVariable String studentNo) {
        Student student = studentService.getByStudentNo(studentNo);
        if (student == null) {
            return Result.error(404, "学生不存在");
        }
        return Result.success(student);
    }

    //查询学生选课情况
    @Operation(summary = "查询学生选课情况", description = "根据学号查询学生已选的所有课程及详细信息")
    @GetMapping("/{studentNo}/selections")
    public Result<List<StudentSelection>> getStudentSelections(@PathVariable String studentNo) {
        Student student = studentService.getByStudentNo(studentNo);
        if (student == null) {
            return Result.error(404, "学生不存在");
        }

        List<Selection_Record> selectionRecords = selectionRecordService.lambdaQuery()
                .eq(Selection_Record::getStudentNo, studentNo)
                .list();

        List<StudentSelection> selectionDTOList = selectionRecords.stream()
                .map(record -> {
                    Course course = courseService.lambdaQuery()
                            .eq(Course::getCourseNo, record.getCourseNo())
                            .one();

                    if (course != null) {
                        return new StudentSelection(
                                course.getCourseNo(),
                                course.getCourseName(),
                                course.getTeacher(),
                                course.getCredit(),
                                course.getTotalCapacity(),
                                course.getRemaining(),
                                record.getSelectTime()
                        );
                    }
                    return null;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());

        return Result.success(selectionDTOList);
    }

}
