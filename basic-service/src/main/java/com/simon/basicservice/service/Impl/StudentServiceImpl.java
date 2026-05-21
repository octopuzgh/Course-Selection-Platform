package com.simon.basicservice.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.simon.basicservice.entity.Student;
import com.simon.basicservice.entity.User;
import com.simon.basicservice.mapper.StudentMapper;
import com.simon.basicservice.service.interf.StudentService;
import com.simon.basicservice.service.interf.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    @Autowired
    private UserService userService;


    @Override
    public Student getByStudentNo(String studentNo) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getStudentNo, studentNo);
        return this.getOne(wrapper);
    }

    /**
     * 创建学生时同步创建对应的用户账户
     */
    @Transactional
    @Override
    public boolean save(Student student) {
        boolean result = super.save(student);
        if (result && student != null) {
            createUserForStudent(student);
        }
        return result;
    }

    /**
     * 删除学生时同步删除对应的用户账户
     */
    @Transactional
    @Override
    public boolean removeById(java.io.Serializable id) {
        // 先获取学生信息，用于删除对应的用户
        Student student = this.getById(id);
        boolean result = super.removeById(id);
        if (result && student != null) {
            deleteUserForStudent(student.getStudentNo());
        }
        return result;
    }

    /**
     * 为学生创建对应的用户账户
     */
    private void createUserForStudent(Student student) {
        if (student == null || student.getStudentNo() == null) {
            return;
        }

        // 检查是否已存在对应用户
        User existingUser = userService.getByUserid(student.getStudentNo());
        if (existingUser != null) {
            return; // 已存在则不重复创建
        }

        // 创建新用户
        User user = new User();
        user.setUserid(student.getStudentNo());
        user.setPassword("123456"); // 默认密码
        user.setRole("STUDENT"); // 默认角色

        userService.save(user);
    }

    /**
     * 删除学生对应的用户账户
     */
    private void deleteUserForStudent(String studentNo) {
        if (studentNo == null) {
            return;
        }

        User user = userService.getByUserid(studentNo);
        if (user != null) {
            userService.removeById(user.getId());
        }
    }

}
