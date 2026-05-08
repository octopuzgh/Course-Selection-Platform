package com.simon.basicservice.controller;

import com.simon.basicservice.common.Result;
import com.simon.basicservice.service.interf.Selection_RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "选课记录管理", description = "选课记录的保存和查询接口")
@RestController
@RequestMapping("/selections")
public class Selection_RecordController {
    @Autowired
    private Selection_RecordService selectionService;

    // 保存选课记录（供Kafka消费者或8081直接调用）
    @Operation(summary = "保存选课记录", description = "保存学生的选课记录，可由Kafka消费者或直接调用")
    @PostMapping
    public Result<String> saveSelection(@RequestBody Map<String, String> body) {
        String studentNo = body.get("studentNo");
        String courseNo = body.get("courseNo");
        if (studentNo == null || courseNo == null) {
            return Result.error("参数缺失");
        }
        selectionService.saveSelection(studentNo, courseNo);
        return Result.success("记录已保存");
    }

    // 检查是否已选
    @Operation(summary = "检查是否已选", description = "检查学生是否已经选择了某门课程")
    @GetMapping("/check")
    public Result<Boolean> checkSelection(@RequestParam String studentNo,
                                          @RequestParam String courseNo) {
        boolean exists = selectionService.hasSelected(studentNo, courseNo);
        return Result.success(exists);
    }

    // 8080自己的完整选课接口（原子操作）
    @Operation(summary = "提交选课", description = "完整的选课操作，包含名额扣减和记录保存的原子操作")
    @PostMapping("/submit")
    public Result<String> submitSelection(@RequestBody Map<String, String> body) {
        String studentNo = body.get("studentNo");
        String courseNo = body.get("courseNo");
        String msg = selectionService.fullSelectCourse(studentNo, courseNo);
        if ("选课成功".equals(msg)) {
            return Result.success(msg);
        } else {
            return Result.error(409, msg);
        }
    }
}
