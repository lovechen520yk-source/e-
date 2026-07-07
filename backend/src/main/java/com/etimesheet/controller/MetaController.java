package com.etimesheet.controller;

import com.etimesheet.service.WorkRecordService;
import com.etimesheet.util.ResponseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 元数据 REST API 控制器
 */
@RestController
@RequestMapping("/api/meta")
public class MetaController {

    private final WorkRecordService workRecordService;

    public MetaController(WorkRecordService workRecordService) {
        this.workRecordService = workRecordService;
    }

    /**
     * 获取所有可选项（项目、公司、部门、岗位列表）
     */
    @GetMapping("/options")
    public ResponseResult<Map<String, List<String>>> getOptions() {
        Map<String, List<String>> options = workRecordService.getAllOptions();
        return ResponseResult.success(options);
    }
}
