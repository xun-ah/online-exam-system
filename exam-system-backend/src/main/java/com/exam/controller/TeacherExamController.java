package com.exam.controller;

import com.exam.annotation.SysLog;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.Exam;
import com.exam.entity.ExamRecord;
import com.exam.entity.Student;
import com.exam.entity.Teacher;
import com.exam.entity.TeacherClass;
import com.exam.mapper.ExamRecordMapper;
import com.exam.mapper.StudentMapper;
import com.exam.mapper.TeacherMapper;
import com.exam.service.ExamService;
import com.exam.service.TeacherClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/teacher/exams")
public class TeacherExamController {
    
    @Autowired
    private ExamService examService;
    
    @Autowired
    private TeacherClassService teacherClassService;
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    @Autowired
    private ExamRecordMapper examRecordMapper;
    
    @Autowired
    private StudentMapper studentMapper;
    
    /**
     * 获取考试列表（只返回本院系的考试）
     */
    @GetMapping
    public Result<List<Map<String, Object>>> getExamList(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestAttribute("userId") Long userId) {
        // 获取教师所属院系ID
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher == null) {
            return Result.error("未找到教师信息");
        }
        
        PageResult<Exam> result = examService.getExamListByDepartment(teacher.getDepartmentId(), pageNum, pageSize);
        
        // 为每个考试添加统计信息
        List<Map<String, Object>> examList = result.getRecords().stream()
            .map(exam -> {
                Map<String, Object> examData = new HashMap<>();
                examData.put("id", exam.getId());
                examData.put("examName", exam.getExamName());
                examData.put("className", exam.getClassName());
                examData.put("paperId", exam.getPaperId());
                examData.put("paperName", exam.getPaperName());
                examData.put("startTime", exam.getStartTime());
                examData.put("endTime", exam.getEndTime());
                examData.put("duration", exam.getDuration());
                examData.put("status", exam.getStatus());
                examData.put("subject", exam.getSubject());
                
                // 统计已完成人数（status >= 1）
                List<ExamRecord> records = examRecordMapper.selectList(exam.getId(), null);
                long completedCount = records.stream()
                    .filter(r -> r.getStatus() != null && r.getStatus() >= 1)
                    .count();
                examData.put("participantCount", completedCount);
                
                // 统计班级总人数
                int totalCount = studentMapper.count(null, null, exam.getClassId(), teacher.getDepartmentId());
                examData.put("totalCount", totalCount);
                
                return examData;
            })
            .collect(Collectors.toList());
        
        return Result.success(examList);
    }
    
    /**
     * 获取考试详情
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getExamById(@PathVariable Long id, @RequestAttribute("userId") Long userId) {
        Exam exam = examService.getExamById(id);
        if (exam == null) {
            return Result.error("考试不存在");
        }
        
        // 获取教师所属院系ID
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher == null) {
            return Result.error("未找到教师信息");
        }
        
        // 构建返回数据
        Map<String, Object> examData = new HashMap<>();
        examData.put("id", exam.getId());
        examData.put("examName", exam.getExamName());
        examData.put("paperId", exam.getPaperId());
        examData.put("paperName", exam.getPaperName());
        examData.put("classId", exam.getClassId());
        examData.put("className", exam.getClassName());
        examData.put("startTime", exam.getStartTime());
        examData.put("endTime", exam.getEndTime());
        examData.put("duration", exam.getDuration());
        examData.put("status", exam.getStatus());
        examData.put("subject", exam.getSubject());
        // 确保 shuffleEnabled 不为 null，默认为 0
        examData.put("shuffleEnabled", exam.getShuffleEnabled() != null ? exam.getShuffleEnabled() : 0);
        
        // 统计已完成人数（status >= 1）
        List<ExamRecord> records = examRecordMapper.selectList(exam.getId(), null);
        long completedCount = records.stream()
            .filter(r -> r.getStatus() != null && r.getStatus() >= 1)
            .count();
        examData.put("participantCount", completedCount);
        
        // 统计班级总人数
        int totalCount = studentMapper.count(null, null, exam.getClassId(), teacher.getDepartmentId());
        examData.put("totalCount", totalCount);
        
        return Result.success(examData);
    }
    
    /**
     * 创建考试
     */
    @SysLog("创建考试")
    @PostMapping
    public Result<Void> createExam(@RequestBody Exam exam, @RequestAttribute("userId") Long userId) {
        System.out.println("[创建考试] 接收到的数据 - shuffleEnabled: " + exam.getShuffleEnabled());
        
        // 通过userId查询教师信息
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher != null) {
            exam.setTeacherId(teacher.getId());
        } else {
            return Result.error("未找到教师信息");
        }
        
        examService.createExam(exam, userId);
        return Result.success("创建成功", null);
    }
    
    /**
     * 更新考试
     */
    @SysLog("更新考试")
    @PutMapping("/{id}")
    public Result<Void> updateExam(@PathVariable Long id, @RequestBody Exam exam) {
        exam.setId(id);
        examService.updateExam(exam);
        return Result.success("更新成功", null);
    }
    
    /**
     * 删除考试
     */
    @SysLog("删除考试")
    @DeleteMapping("/{id}")
    public Result<Void> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return Result.success("删除成功", null);
    }
    
    /**
     * 获取当前教师负责的班级列表（用于发布考试时选择）
     */
    @GetMapping("/my-classes")
    public Result<List<TeacherClass>> getMyClasses(@RequestAttribute("userId") Long userId) {
        // 通过userId查询教师信息
        Teacher teacher = teacherMapper.selectByUserId(userId);
        if (teacher == null) {
            return Result.error("未找到教师信息");
        }
        
        List<TeacherClass> classes = teacherClassService.getClassesByTeacherId(teacher.getId());
        
        // 按班级ID去重，保留每个班级的一条记录
        List<TeacherClass> distinctClasses = classes.stream()
            .collect(Collectors.toMap(
                TeacherClass::getClassId, 
                c -> c, 
                (existing, replacement) -> existing
            ))
            .values()
            .stream()
            .collect(Collectors.toList());
        
        return Result.success(distinctClasses);
    }
    
    /**
     * 获取考试监控数据
     */
    @GetMapping("/{id}/monitor")
    public Result<Map<String, Object>> getExamMonitor(@PathVariable Long id) {
        // 获取考试信息
        Exam exam = examService.getExamById(id);
        if (exam == null) {
            return Result.error("考试不存在");
        }
        
        // 获取该考试对应班级的学生总数
        int totalCount = 0;
        if (exam.getClassId() != null) {
            totalCount = studentMapper.count(null, null, exam.getClassId(), null);
        }
        
        // 查询该考试的考试记录
        List<ExamRecord> records = examRecordMapper.selectList(id, null);
        int submittedCount = 0;
        int examiningCount = 0;
        int abnormalCount = 0;
        
        List<Map<String, Object>> students = new ArrayList<>();
        
        for (ExamRecord record : records) {
            Map<String, Object> studentData = new HashMap<>();
            
            // 获取学生信息
            Student student = studentMapper.selectById(record.getStudentId());
            if (student != null) {
                studentData.put("studentNo", student.getStudentNo());
                studentData.put("realName", student.getRealName());
            }
            
            studentData.put("id", record.getId());
            studentData.put("status", record.getStatus());
            
            // 状态文本
            String statusText = "考试中";
            if (record.getStatus() != null) {
                if (record.getStatus() >= 1) {
                    statusText = "已交卷";
                    submittedCount++;
                } else {
                    examiningCount++;
                }
            }
            studentData.put("statusText", statusText);
            
            // 进度（假设已答题数）
            int progress = 0;
            if (record.getAnswers() != null) {
                try {
                    cn.hutool.json.JSONObject answers = cn.hutool.json.JSONUtil.parseObj(record.getAnswers());
                    progress = (int) (answers.size() * 100.0 / 20); // 假设20道题
                    if (progress > 100) progress = 100;
                } catch (Exception e) {
                    progress = 0;
                }
            }
            studentData.put("progress", progress);
            
            // 异常检测
            int switchCount = record.getSwitchCount() != null ? record.getSwitchCount() : 0;
            boolean isAbnormal = false;
            String abnormalReason = "";
            
            // 切屏次数超过3次视为异常
            if (switchCount > 3) {
                isAbnormal = true;
                abnormalReason = "切屏" + switchCount + "次";
            }
            
            // 检查是否被强制交卷
            if (record.getSubmitTime() != null && exam.getEndTime() != null) {
                // 如果提前交卷超过30分钟，可能异常
                long minutesBeforeEnd = java.time.Duration.between(record.getSubmitTime(), exam.getEndTime()).toMinutes();
                if (minutesBeforeEnd > 30 && record.getStatus() >= 1) {
                    isAbnormal = true;
                    abnormalReason = (abnormalReason.isEmpty() ? "" : abnormalReason + "; ") + "提前交卷";
                }
            }
            
            if (isAbnormal) {
                abnormalCount++;
            }
            
            studentData.put("abnormal", isAbnormal);
            studentData.put("abnormalReason", abnormalReason);
            studentData.put("switchCount", switchCount);
            
            students.add(studentData);
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("totalCount", totalCount);
        data.put("submittedCount", submittedCount);
        data.put("examiningCount", examiningCount);
        data.put("abnormalCount", abnormalCount);
        data.put("students", students);
        
        return Result.success(data);
    }
    
    /**
     * 强制学生交卷
     */
    @SysLog("强制交卷")
    @PostMapping("/{examId}/force-submit/{studentId}")
    public Result<Void> forceSubmit(@PathVariable Long examId, 
                                     @PathVariable Long studentId,
                                     @RequestAttribute("userId") Long userId) {
        try {
            // 查询该学生的考试记录
            List<ExamRecord> records = examRecordMapper.selectList(examId, studentId);
            if (records == null || records.isEmpty()) {
                return Result.error("该学生尚未开始考试");
            }
            
            ExamRecord record = records.get(0);
            
            // 检查是否已经交卷
            if (record.getStatus() != null && record.getStatus() >= 1) {
                return Result.error("该学生已经交卷");
            }
            
            // 更新状态为已交卷（status=1）
            record.setStatus(1);
            record.setSubmitTime(java.time.LocalDateTime.now());
            examRecordMapper.updateById(record);
            
            return Result.success("已强制 " + record.getStudentId() + " 交卷", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("强制交卷失败：" + e.getMessage());
        }
    }
    
    /**
     * 延长考试时间
     */
    @SysLog("延长考试时间")
    @PostMapping("/extend-time")
    public Result<Void> extendExamTime(@RequestBody Map<String, Object> params, @RequestAttribute("userId") Long userId) {
        try {
            // 获取参数
            Long examId = params.get("examId") != null ? ((Number) params.get("examId")).longValue() : null;
            Long studentId = params.get("studentId") != null ? ((Number) params.get("studentId")).longValue() : null;
            Integer extendMinutes = params.get("extendMinutes") != null ? ((Number) params.get("extendMinutes")).intValue() : null;
            
            if (examId == null || studentId == null || extendMinutes == null) {
                return Result.error("参数不完整");
            }
            
            // 查询该学生的考试记录
            List<ExamRecord> records = examRecordMapper.selectList(examId, studentId);
            if (records == null || records.isEmpty()) {
                return Result.error("该学生尚未开始考试，无法延长考试时间");
            }
            
            // 获取最新的考试记录
            ExamRecord record = records.get(0);
            
            // 更新延长考试时间
            int currentExtra = record.getExtraMinutes() != null ? record.getExtraMinutes() : 0;
            record.setExtraMinutes(currentExtra + extendMinutes);
            examRecordMapper.updateById(record);
            
            // 同时更新考试的结束时间（在原有基础上增加延长分钟数）
            Exam exam = examService.getExamById(examId);
            if (exam != null && exam.getEndTime() != null) {
                exam.setEndTime(exam.getEndTime().plusMinutes(extendMinutes));
                examService.updateExam(exam);
            }
            
            return Result.success("延长成功，已为该学生延长" + extendMinutes + "分钟", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("延长考试时间失败：" + e.getMessage());
        }
    }
}
