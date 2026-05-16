package com.simon.basicservice.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.simon.basicservice.entity.Course;
import com.simon.basicservice.mapper.CourseMapper;
import com.simon.basicservice.service.interf.CourseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    @Override
    public Course getByCourseNo(String courseNo) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Course::getCourseNo, courseNo);
        return this.getOne(wrapper);
    }

    @Override
    public List<Course> listAll() {
        return this.list();
    }


    //原子扣减名额：使用数据库行级锁 + 更新条件 remaining > 0，保证并发安全
    @Override
    @Transactional
    public Course decrementRemaining(String courseNo) {
        // 先查询课程，验证存在并获取当前名额
        Course course = getByCourseNo(courseNo);
        if (course == null || course.getRemaining() == null || course.getRemaining() <= 0) {
            return null;
        }

        // 使用条件更新 remaining = remaining - 1，仅当 remaining > 0 时执行
        LambdaUpdateWrapper<Course> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Course::getCourseNo, courseNo)
                .gt(Course::getRemaining, 0)
                .setSql("remaining = remaining - 1, selected_count = selected_count + 1");

        int rows = baseMapper.update(null, updateWrapper);
        if (rows == 0) {
            return null; // 库存不足或并发冲突
        }

        // 更新成功后重新查询返回最新状态
        return getByCourseNo(courseNo);
    }

    @Override
    @Transactional
    public Course incrementRemaining(String courseNo) {
        Course course = getByCourseNo(courseNo);
        if (course == null) {
            return null;
        }

        LambdaUpdateWrapper<Course> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Course::getCourseNo, courseNo)
                .lt(Course::getSelectedCount, course.getTotalCapacity())
                .setSql("remaining = remaining + 1, selected_count = selected_count - 1");

        int rows = baseMapper.update(null, updateWrapper);
        if (rows == 0) {
            return null;
        }

        return getByCourseNo(courseNo);
    }

}
