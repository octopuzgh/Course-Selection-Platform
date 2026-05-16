package com.simon.basicservice.controller;

import com.simon.basicservice.annotation.RequireRole;
import com.simon.basicservice.common.ErrorCode;
import com.simon.basicservice.common.Result;
import com.simon.basicservice.dto.SelectionRequest;
import com.simon.basicservice.service.interf.Selection_RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "选课记录管理", description = "选课记录的保存和查询接口")
@RestController
@RequestMapping("/selections")
public class Selection_RecordController {
    @Autowired
    private Selection_RecordService selectionService;

    @Operation(summary = "保存选课记录", description = "保存学生的选课记录，可由Kafka消费者或直接调用")
    @RequireRole(RequireRole.ADMIN)
    @PostMapping
    public Result<String> saveSelection(@Valid @RequestBody SelectionRequest request) {
        selectionService.saveSelection(request.getStudentNo(), request.getCourseNo());
        return Result.success("记录已保存");
    }

    @Operation(summary = "检查是否已选", description = "检查学生是否已经选择了某门课程")
    @GetMapping("/check")
    public Result<Boolean> checkSelection(@RequestParam String studentNo,
                                          @RequestParam String courseNo) {
        boolean exists = selectionService.hasSelected(studentNo, courseNo);
        return Result.success(exists);
    }

    @Operation(summary = "提交选课", description = "完整的选课操作，包含名额扣减和记录保存的原子操作")
    @RequireRole({RequireRole.ADMIN, RequireRole.STUDENT})
    @PostMapping("/submit")
    public Result<String> submitSelection(@Valid @RequestBody SelectionRequest request) {
        String msg = selectionService.fullSelectCourse(request.getStudentNo(), request.getCourseNo());
        if ("选课成功".equals(msg)) {
            return Result.success(msg);
        } else {
            return Result.error(ErrorCode.COURSE_ALREADY_SELECTED, msg);
        }
    }

    @Operation(summary = "退课", description = "学生退课操作，包含记录删除和名额恢复的原子操作")
    @RequireRole({RequireRole.ADMIN, RequireRole.STUDENT})
    @PostMapping("/drop")
    public Result<String> dropCourse(@Valid @RequestBody SelectionRequest request) {
        String msg = selectionService.dropCourse(request.getStudentNo(), request.getCourseNo());
        if ("退课成功".equals(msg)) {
            return Result.success(msg);
        } else {
            return Result.error(ErrorCode.BUSINESS_ERROR, msg);
        }
    }

}
