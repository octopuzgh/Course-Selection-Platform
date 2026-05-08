package com.simon.basicservice.service.interf;

import com.baomidou.mybatisplus.extension.service.IService;
import com.simon.basicservice.entity.Course;

import java.util.List;

public interface CourseService extends IService<Course> {

    //根据课程号查询课程
    Course getByCourseNo(String courseNo);

    //获取所有课程列表
    List<Course> listAll();


    //原子扣减名额（返回更新后的课程，失败返回 null）
    Course decrementRemaining(String courseNo);
}
