package com.exam.service;

import com.exam.entity.ClassInfo;
import com.exam.entity.Subject;
import com.exam.entity.Teacher;
import com.exam.entity.TeacherClass;
import com.exam.mapper.ClassMapper;
import com.exam.mapper.SubjectMapper;
import com.exam.mapper.TeacherClassMapper;
import com.exam.mapper.TeacherMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeacherClassService {
    
    @Autowired
    private TeacherClassMapper teacherClassMapper;
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    @Autowired
    private ClassMapper classMapper;
    
    @Autowired
    private SubjectMapper subjectMapper;
    
    /**
     * 查询教师负责的班级列表
     */
    public List<TeacherClass> getClassesByTeacherId(Long teacherId) {
        return teacherClassMapper.selectByTeacherId(teacherId);
    }
    
    /**
     * 查询班级的任课教师列表
     */
    public List<TeacherClass> getTeachersByClassId(Long classId) {
        return teacherClassMapper.selectByClassId(classId);
    }
    
    /**
     * 为教师分配班级
     */
    @Transactional
    public void assignClassToTeacher(TeacherClass teacherClass) {
        // 检查是否已经存在相同的老师-班级-科目组合
        List<TeacherClass> existingList = teacherClassMapper.selectByTeacherId(teacherClass.getTeacherId());
        boolean exists = existingList.stream().anyMatch(tc -> 
            tc.getClassId().equals(teacherClass.getClassId()) && 
            tc.getSubjectId().equals(teacherClass.getSubjectId())
        );
        
        if (exists) {
            throw new RuntimeException("该教师在该班级已经负责此科目");
        }
        
        teacherClassMapper.insert(teacherClass);
    }
    
    /**
     * 批量为教师分配班级
     */
    @Transactional
    public void batchAssignClasses(Long teacherId, List<Long> classIds, String subjectName) {
        // 获取教师信息
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师不存在");
        }
            
        System.out.println("=== 开始分配班级 ===");
        System.out.println("教师ID: " + teacherId);
        System.out.println("教师姓名: " + teacher.getRealName());
        System.out.println("教师院系ID: " + teacher.getDepartmentId());
        System.out.println("选择的科目名称: " + subjectName);
        System.out.println("选择的班级IDs: " + classIds);
            
        // 根据科目名称获取科目ID
        final Long subjectId;
        if (subjectName != null && !subjectName.isEmpty()) {
            // 查询该院系下所有启用的科目
            List<Subject> subjects = subjectMapper.selectActiveList(teacher.getDepartmentId());
            System.out.println("该院系下的科目列表: " + subjects.stream().map(Subject::getName).collect(java.util.stream.Collectors.toList()));
                
            Subject subject = subjects.stream()
                .filter(s -> {
                    boolean match = s.getName().equals(subjectName);
                    System.out.println("对比科目: '" + s.getName() + "' 与 '" + subjectName + "' = " + match);
                    return match;
                })
                .findFirst()
                .orElse(null);
                
            if (subject == null) {
                System.err.println("错误: 找不到匹配的科目 '" + subjectName + "'");
                throw new RuntimeException("科目不存在或未启用: " + subjectName + "。请检查该科目是否在院系科目列表中且状态为启用。");
            }
            subjectId = subject.getId();
            System.out.println("匹配成功! 科目ID: " + subjectId);
        } else {
            subjectId = null;
            System.out.println("未选择科目");
        }
            
        // 不再删除旧记录，改为增量添加
        // 获取该教师已有的班级科目记录
        List<TeacherClass> existingRecords = teacherClassMapper.selectByTeacherId(teacherId);
        System.out.println("该教师已有记录数: " + existingRecords.size());
            
        // 批量添加新的关联,验证班级是否属于教师所在院系
        if (classIds != null && !classIds.isEmpty()) {
            List<TeacherClass> list = classIds.stream()
                .map(classId -> {
                    // 获取班级信息
                    ClassInfo classInfo = classMapper.selectById(classId);
                    if (classInfo == null) {
                        throw new RuntimeException("班级不存在:" + classId);
                    }
                        
                    // 验证班级是否属于教师所在院系
                    if (!classInfo.getDepartmentId().equals(teacher.getDepartmentId())) {
                        throw new RuntimeException(
                            String.format("班级[%s]不属于教师所在院系[%s],不能分配",
                                classInfo.getClassName(),
                                teacher.getDepartmentName())
                        );
                    }
                    
                    // 检查是否已存在相同的（教师-班级-科目）组合
                    boolean exists = existingRecords.stream().anyMatch(tc -> 
                        tc.getClassId().equals(classId) && 
                        tc.getSubjectId().equals(subjectId)
                    );
                    
                    if (exists) {
                        System.out.println("跳过已存在的记录: classId=" + classId + ", subjectId=" + subjectId);
                        return null; // 返回null，后面过滤掉
                    }
                        
                    TeacherClass tc = new TeacherClass();
                    tc.setTeacherId(teacherId);
                    tc.setClassId(classId);
                    tc.setSubjectId(subjectId);
                    tc.setSubject(subjectName); // 同时保存科目名称
                    System.out.println("准备插入: teacherId=" + teacherId + ", classId=" + classId + ", subjectId=" + subjectId + ", subject=" + subjectName);
                    return tc;
                })
                .filter(tc -> tc != null) // 过滤掉已存在的记录
                .collect(java.util.stream.Collectors.toList());
            
            if (!list.isEmpty()) {
                int insertCount = teacherClassMapper.batchInsert(list);
                System.out.println("批量插入成功! 插入记录数: " + insertCount);
            } else {
                System.out.println("所有记录已存在，无需插入");
            }
        }
            
        System.out.println("=== 分配班级完成 ===");
    }
    
    /**
     * 移除教师的班级
     */
    @Transactional
    public void removeClassFromTeacher(Long id) {
        teacherClassMapper.deleteById(id);
    }
    
    /**
     * 检查教师是否负责某个班级
     */
    public boolean isTeacherResponsibleForClass(Long teacherId, Long classId) {
        TeacherClass tc = teacherClassMapper.selectByTeacherAndClass(teacherId, classId);
        return tc != null;
    }
}
