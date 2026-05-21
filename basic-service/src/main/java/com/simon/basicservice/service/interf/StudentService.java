package com.simon.basicservice.service.interf;

import com.baomidou.mybatisplus.extension.service.IService;
import com.simon.basicservice.entity.Student;

public interface StudentService extends IService<Student> {

    //根据学号查询学生
    Student getByStudentNo(String studentNo);

    // 重写父类方法以支持事务控制
    boolean save(Student student);
    boolean removeById(java.io.Serializable id);
}
