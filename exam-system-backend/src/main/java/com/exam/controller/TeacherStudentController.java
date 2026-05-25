package com.exam.controller;



import com.exam.annotation.SysLog;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.Student;
import com.exam.entity.ExamRecord;
import com.exam.entity.Department;
import com.exam.entity.ClassInfo;
import com.exam.entity.User;
import com.exam.entity.Exam;
import com.exam.mapper.StudentMapper;
import com.exam.mapper.ExamRecordMapper;
import com.exam.mapper.ExamMapper;
import com.exam.mapper.TeacherClassMapper;
import com.exam.mapper.TeacherMapper;
import com.exam.mapper.DepartmentMapper;
import com.exam.mapper.ClassMapper;
import com.exam.mapper.UserMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教师端学生管理Controller
 */
@RestController
@RequestMapping("/teacher/students")
public class TeacherStudentController {
    
    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private ExamRecordMapper examRecordMapper;
    
    @Autowired
    private ExamMapper examMapper;
    
    @Autowired
    private TeacherClassMapper teacherClassMapper;
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    @Autowired
    private DepartmentMapper departmentMapper;
    
    @Autowired
    private ClassMapper classMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 获取学生列表(教师所属院系的学生)
     */
    @GetMapping
    public Result<PageResult<Map<String, Object>>> getStudentList(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) String studentNo,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestAttribute("userId") Long userId) {
        
        // 获取教师所属院系ID
        var teacher = teacherMapper.selectByUserId(userId);
        if (teacher == null) {
            return Result.error("未找到教师信息");
        }
        
        // 如果没有指定院系，默认使用教师所属院系
        Long targetDepartmentId = departmentId != null ? departmentId : teacher.getDepartmentId();
        
        // 查询本院系的所有学生
        List<Student> allStudents = studentMapper.selectList(studentNo, studentName, classId, targetDepartmentId, 0, 10000);
        
        // 关联查询班级和院系名称
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Student student : allStudents) {
            Map<String, Object> studentMap = new HashMap<>();
            studentMap.put("id", student.getId());
            studentMap.put("studentNo", student.getStudentNo());
            studentMap.put("realName", student.getRealName());
            studentMap.put("gender", student.getGender());
            studentMap.put("phone", student.getPhone());
            studentMap.put("email", student.getEmail());
            studentMap.put("classId", student.getClassId());
            studentMap.put("departmentId", student.getDepartmentId());
            studentMap.put("className", student.getClassName());
            studentMap.put("departmentName", student.getDepartmentName());
            
            resultList.add(studentMap);
        }
        
        // 分页
        int total = resultList.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        
        List<Map<String, Object>> pageList = fromIndex < total ? resultList.subList(fromIndex, toIndex) : new ArrayList<>();
        
        PageResult<Map<String, Object>> pageResult = new PageResult<>((long) total, pageList);
        
        return Result.success(pageResult);
    }
    
    /**
     * 获取学生详情
     */
    @GetMapping("/{id}")
    public Result<Student> getStudentById(@PathVariable Long id) {
        Student student = studentMapper.selectById(id);
        return Result.success(student);
    }
    
    /**
     * 获取学生考试成绩列表
     */
    @GetMapping("/{id}/scores")
    public Result<List<Map<String, Object>>> getStudentScores(@PathVariable Long id) {
        List<Map<String, Object>> scoreList = new ArrayList<>();
        
        // 获取该学生的所有考试记录
        List<ExamRecord> records = examRecordMapper.selectList(null, id);
        
        for (ExamRecord record : records) {
            Map<String, Object> scoreData = new HashMap<>();
            scoreData.put("id", record.getId());
            scoreData.put("examId", record.getExamId());
            scoreData.put("score", record.getScore());
            scoreData.put("submitTime", record.getSubmitTime());
            scoreData.put("status", record.getStatus());
            
            // 获取考试信息
            var exam = examMapper.selectById(record.getExamId());
            if (exam != null) {
                scoreData.put("examName", exam.getExamName());
                // 简化处理:从考试名称中提取科目
                String subject = exam.getExamName();
                if (subject.contains("》")) {
                    subject = subject.substring(1, subject.indexOf("》"));
                }
                scoreData.put("subject", subject);
            }
            
            // TODO: 计算排名(需要查询该考试所有学生的成绩)
            scoreData.put("rank", 0);
            
            scoreList.add(scoreData);
        }
        
        // 按提交时间降序排序
        scoreList.sort((a, b) -> {
            var timeA = (java.time.LocalDateTime) a.get("submitTime");
            var timeB = (java.time.LocalDateTime) b.get("submitTime");
            if (timeA == null && timeB == null) return 0;
            if (timeA == null) return 1;
            if (timeB == null) return -1;
            return timeB.compareTo(timeA);
        });
        
        return Result.success(scoreList);
    }
    
    /**
     * 导出学生成绩
     */
    @SysLog("导出学生成绩")
    @GetMapping("/export")
    public void exportScores(
            @RequestParam(required = false) Long classId,
            @RequestAttribute("userId") Long userId,
            HttpServletResponse response) {
        try {
            // 获取教师所属院系
            var teacher = teacherMapper.selectByUserId(userId);
            if (teacher == null) {
                response.setStatus(400);
                return;
            }
            
            // 查询学生列表
            List<Student> students = studentMapper.selectList(null, null, classId, teacher.getDepartmentId(), 0, 10000);
            
            if (students == null || students.isEmpty()) {
                response.setStatus(404);
                return;
            }
            
            // 创建Excel工作簿
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("学生成绩表");
            
            // 创建标题样式
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"学号", "姓名", "性别", "班级", "院系", "联系电话", "考试次数", "最高分", "最低分", "平均分"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 填充数据
            for (int i = 0; i < students.size(); i++) {
                Student student = students.get(i);
                Row row = sheet.createRow(i + 1);
                
                row.createCell(0).setCellValue(student.getStudentNo());
                row.createCell(1).setCellValue(student.getRealName());
                row.createCell(2).setCellValue(student.getGender() == 1 ? "男" : "女");
                row.createCell(3).setCellValue(student.getClassName() != null ? student.getClassName() : "");
                row.createCell(4).setCellValue(student.getDepartmentName() != null ? student.getDepartmentName() : "");
                row.createCell(5).setCellValue(student.getPhone() != null ? student.getPhone() : "");
                
                // 查询该学生的所有考试记录
                List<ExamRecord> records = examRecordMapper.selectList(null, student.getId());
                
                int examCount = 0;
                double maxScore = 0;
                double minScore = 100;
                double totalScore = 0;
                
                for (ExamRecord record : records) {
                    if (record.getStatus() != null && record.getStatus() >= 2 && record.getScore() != null) {
                        examCount++;
                        double score = record.getScore().doubleValue();
                        totalScore += score;
                        if (score > maxScore) maxScore = score;
                        if (score < minScore) minScore = score;
                    }
                }
                
                double avgScore = examCount > 0 ? totalScore / examCount : 0;
                
                row.createCell(6).setCellValue(examCount);
                row.createCell(7).setCellValue(examCount > 0 ? maxScore : 0);
                row.createCell(8).setCellValue(examCount > 0 ? minScore : 0);
                row.createCell(9).setCellValue(String.format("%.2f", avgScore));
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("学生成绩_" + System.currentTimeMillis(), "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            
            // 写入响应
            try (OutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            } finally {
                workbook.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
        }
    }
    
    /**
     * 获取教师所属院系信息
     */
    @GetMapping("/department")
    public Result<Department> getTeacherDepartment(@RequestAttribute("userId") Long userId) {
        // 获取教师信息
        var teacher = teacherMapper.selectByUserId(userId);
        if (teacher == null) {
            return Result.error("未找到教师信息");
        }
        
        // 获取院系信息
        Department department = departmentMapper.selectById(teacher.getDepartmentId());
        if (department == null) {
            return Result.error("未找到院系信息");
        }
        
        return Result.success(department);
    }
    
    /**
     * 获取本院系下的班级列表
     */
    @GetMapping("/classes")
    public Result<List<ClassInfo>> getClassListByDepartment(
            @RequestParam(required = false) Long departmentId,
            @RequestAttribute("userId") Long userId) {
        
        Long targetDepartmentId = departmentId;
        
        // 如果没有指定院系ID，使用教师所属院系
        if (targetDepartmentId == null) {
            var teacher = teacherMapper.selectByUserId(userId);
            if (teacher == null) {
                return Result.error("未找到教师信息");
            }
            targetDepartmentId = teacher.getDepartmentId();
        }
        
        // 查询该院系下的所有班级
        List<ClassInfo> classes = classMapper.selectList(targetDepartmentId);
        
        return Result.success(classes);
    }
    
    /**
     * 新增学生
     */
    @SysLog("新增学生")
    @PostMapping
    public Result<String> addStudent(@RequestBody Student student, @RequestAttribute("userId") Long userId) {
        try {
            // 获取教师所属院系ID
            var teacher = teacherMapper.selectByUserId(userId);
            if (teacher == null) {
                return Result.error("未找到教师信息");
            }
            
            // 设置默认值
            if (student.getDepartmentId() == null) {
                student.setDepartmentId(teacher.getDepartmentId());
            }
            
            // 先创建user账号
            User user = new User();
            user.setUsername(student.getStudentNo()); // 用户名=学号
            user.setPassword("123456"); // 默认密码
            user.setRealName(student.getRealName());
            user.setPhone(student.getPhone());
            user.setEmail(student.getEmail());
            user.setRole(3); // 3-学生
            user.setStatus(1); // 1-正常
            
            userMapper.insert(user);
            
            // 关联userId到student表
            student.setUserId(user.getId());
            studentMapper.insert(student);
            
            return Result.success("新增学生成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("新增学生失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新学生信息
     */
    @SysLog("更新学生信息")
    @PutMapping("/{id}")
    public Result<String> updateStudent(@PathVariable Long id, @RequestBody Student student) {
        try {
            student.setId(id);
            studentMapper.updateById(student);
            return Result.success("更新学生信息成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新学生信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除学生
     */
    @SysLog("删除学生")
    @DeleteMapping("/{id}")
    public Result<String> deleteStudent(@PathVariable Long id) {
        try {
            studentMapper.deleteById(id);
            return Result.success("删除学生成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除学生失败: " + e.getMessage());
        }
    }
}
