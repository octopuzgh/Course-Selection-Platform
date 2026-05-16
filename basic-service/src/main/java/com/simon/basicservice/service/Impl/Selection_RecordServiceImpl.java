package com.simon.basicservice.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.simon.basicservice.entity.Course;
import com.simon.basicservice.entity.Selection_Record;
import com.simon.basicservice.entity.Student;
import com.simon.basicservice.mapper.Selection_RecordMapper;
import com.simon.basicservice.service.interf.CourseService;
import com.simon.basicservice.service.interf.Selection_RecordService;
import com.simon.basicservice.service.interf.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class Selection_RecordServiceImpl extends ServiceImpl<Selection_RecordMapper, Selection_Record> implements Selection_RecordService {

    @Autowired
    private StudentService studentService;

    @Autowired
    private CourseService courseService;

    @Override
    public boolean hasSelected(String studentNo, String courseNo) {
        LambdaQueryWrapper<Selection_Record> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Selection_Record::getStudentNo, studentNo)
                .eq(Selection_Record::getCourseNo, courseNo);
        return this.count(wrapper) > 0;
    }

    @Override
    public void saveSelection(String studentNo, String courseNo) {
        Selection_Record record = new Selection_Record();
        record.setStudentNo(studentNo);
        record.setCourseNo(courseNo);
        record.setSelectTime(LocalDateTime.now());
        this.save(record);
    }

    @Override
    @Transactional
    public String fullSelectCourse(String studentNo, String courseNo) {
        // 1. 验证学生存在
        Student student = studentService.getByStudentNo(studentNo);
        if (student == null) {
            return "学生不存在";
        }

        // 2. 验证课程存在
        Course course = courseService.getByCourseNo(courseNo);
        if (course == null) {
            return "课程不存在";
        }

        // 3. 检查是否已选
        if (hasSelected(studentNo, courseNo)) {
            return "已选过该课程";
        }

        // 4. 原子扣减库存（内部处理并发）
        Course updated = courseService.decrementRemaining(courseNo);
        if (updated == null) {
            return "选课失败，库存不足或系统繁忙";
        }

        // 5. 插入选课记录（如果这里失败，事务会回滚扣库存）
        saveSelection(studentNo, courseNo);

        return "选课成功";
    }

    @Override
    @Transactional
    public String dropCourse(String studentNo, String courseNo) {
        Student student = studentService.getByStudentNo(studentNo);
        if (student == null) {
            return "学生不存在";
        }

        Course course = courseService.getByCourseNo(courseNo);
        if (course == null) {
            return "课程不存在";
        }

        if (!hasSelected(studentNo, courseNo)) {
            return "未选过该课程，无法退课";
        }

        LambdaQueryWrapper<Selection_Record> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Selection_Record::getStudentNo, studentNo)
                .eq(Selection_Record::getCourseNo, courseNo);
        boolean removed = this.remove(wrapper);

        if (!removed) {
            return "退课失败，请稍后重试";
        }

        Course updated = courseService.incrementRemaining(courseNo);
        if (updated == null) {
            return "退课部分成功：记录已删除，但名额恢复失败，请联系管理员";
        }

        return "退课成功";
    }

}
