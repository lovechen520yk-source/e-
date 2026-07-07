package com.etimesheet.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.etimesheet.entity.WorkRecord;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 使用EasyExcel导出工时记录的Excel文件工具类
 */
@Component
public class ExcelUtil {

    /**
     * 导出工时记录为Excel文件
     *
     * @param records   工时记录列表
     * @param yearMonth 年月
     * @param response  HTTP响应
     */
    public void exportWorkRecords(List<WorkRecord> records, String yearMonth, HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("工时记录_" + yearMonth, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            List<WorkRecordExcelVO> voList = new ArrayList<>();
            for (WorkRecord record : records) {
                WorkRecordExcelVO vo = new WorkRecordExcelVO();
                vo.setWorkDate(record.getWorkDate() != null
                        ? record.getWorkDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        : "");
                vo.setCompany(record.getCompany());
                vo.setDepartment(record.getDepartment());
                vo.setPosition(record.getPosition());
                vo.setContent(record.getContent());
                vo.setHours(record.getHours());
                vo.setProject(record.getProject());
                vo.setRemark(record.getRemark());
                voList.add(vo);
            }

            EasyExcel.write(response.getOutputStream(), WorkRecordExcelVO.class)
                    .sheet("工时记录")
                    .doWrite(voList);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    /**
     * Excel导出用的VO类
     */
    @Data
    static class WorkRecordExcelVO {
        @ExcelProperty("工作日期")
        private String workDate;

        @ExcelProperty("公司")
        private String company;

        @ExcelProperty("部门")
        private String department;

        @ExcelProperty("岗位")
        private String position;

        @ExcelProperty("工作内容")
        private String content;

        @ExcelProperty("工时")
        private BigDecimal hours;

        @ExcelProperty("项目")
        private String project;

        @ExcelProperty("备注")
        private String remark;
    }
}
