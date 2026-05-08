package com.exam.service;

import com.exam.common.PageResult;
import com.exam.entity.ClassInfo;
import com.exam.entity.Department;
import com.exam.entity.Student;
import com.exam.entity.User;
import com.exam.mapper.ClassMapper;
import com.exam.mapper.DepartmentMapper;
import com.exam.mapper.StudentMapper;
import com.exam.mapper.UserMapper;
import cn.hutool.crypto.digest.BCrypt;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@Service
public class StudentService {
    
    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private DepartmentMapper departmentMapper;
    
    @Autowired
    private ClassMapper classMapper;
    
    public PageResult<Student> getStudentList(String studentNo, String realName, Long classId, Long departmentId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Student> list = studentMapper.selectList(studentNo, realName, classId, departmentId, offset, pageSize);
        int total = studentMapper.count(studentNo, realName, classId, departmentId);
        return new PageResult<>((long) total, list);
    }
    
    @Transactional
    public void createStudent(Student student) {
        // 创建用户账号
        User user = new User();
        user.setUsername(student.getStudentNo());
        user.setPassword(BCrypt.hashpw("123456")); // 默认密码
        user.setRealName(student.getRealName());
        user.setRole(3); // 学生角色
        user.setPhone(student.getPhone());
        user.setEmail(student.getEmail());
        user.setStatus(1);
        userMapper.insert(user);
        
        student.setUserId(user.getId());
        studentMapper.insert(student);
    }
    
    @Transactional
    public void updateStudent(Student student) {
        studentMapper.updateById(student);
        
        // 同步更新用户信息
        User user = userMapper.selectById(student.getUserId());
        if (user != null) {
            user.setRealName(student.getRealName());
            user.setPhone(student.getPhone());
            user.setEmail(student.getEmail());
            userMapper.updateById(user);
        }
    }
    
    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentMapper.selectById(id);
        if (student != null) {
            studentMapper.deleteById(id);
            userMapper.deleteById(student.getUserId());
        }
    }
    
    /**
     * 从Excel导入学生
     */
    @Transactional
    public String importStudentsFromExcel(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new Exception("文件不能为空");
        }
        
        InputStream inputStream = file.getInputStream();
        Workbook workbook;
        String fileName = file.getOriginalFilename();
        
        // 根据文件类型创建工作簿
        if (fileName.endsWith(".xlsx")) {
            workbook = new XSSFWorkbook(inputStream);
        } else if (fileName.endsWith(".xls")) {
            workbook = new HSSFWorkbook(inputStream);
        } else {
            throw new Exception("只支持Excel文件格式(.xlsx或.xls)");
        }
        
        Sheet sheet = workbook.getSheetAt(0);
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();
        
        // 从第二行开始读取数据（第一行是标题）
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            try {
                // 读取数据（按照Excel列顺序：学号、姓名、性别、班级、院系、手机号、邮箱）
                String studentNo = getCellValue(row.getCell(0));
                String realName = getCellValue(row.getCell(1));
                String genderStr = getCellValue(row.getCell(2));
                String className = getCellValue(row.getCell(3)); // 班级名称
                String departmentName = getCellValue(row.getCell(4)); // 院系名称
                String phone = getCellValue(row.getCell(5));
                String email = getCellValue(row.getCell(6));
                
                // 验证必填字段
                if (studentNo == null || studentNo.trim().isEmpty()) {
                    errors.add("第" + (i + 1) + "行：学号不能为空");
                    failCount++;
                    continue;
                }
                if (realName == null || realName.trim().isEmpty()) {
                    errors.add("第" + (i + 1) + "行：姓名不能为空");
                    failCount++;
                    continue;
                }
                
                // 解析班级和院系名称
                className = className != null ? className.trim() : "";
                departmentName = departmentName != null ? departmentName.trim() : "";
                
                // 处理班级名称中的分隔符（如"外语2021:外国语学院"）
                if (className.contains(":")) {
                    className = className.split(":")[0].trim();
                }
                
                // 根据院系名称查找院系ID
                Long departmentId = null;
                if (departmentName != null && !departmentName.isEmpty()) {
                    List<Department> deptList = departmentMapper.selectList();
                    for (Department dept : deptList) {
                        if (dept.getDeptName().equals(departmentName)) {
                            departmentId = dept.getId();
                            break;
                        }
                    }
                }
                
                // 根据班级名称查找班级ID
                Long classId = null;
                if (className != null && !className.isEmpty()) {
                    List<ClassInfo> classList = classMapper.selectList(departmentId);
                    for (ClassInfo cls : classList) {
                        if (cls.getClassName().equals(className)) {
                            classId = cls.getId();
                            break;
                        }
                    }
                }
                
                // 创建学生对象
                Student student = new Student();
                student.setStudentNo(studentNo.trim());
                student.setRealName(realName.trim());
                student.setGender("男".equals(genderStr) ? 1 : 0);
                student.setPhone(phone != null ? phone.trim() : "");
                student.setEmail(email != null ? email.trim() : "");
                student.setDepartmentId(departmentId);
                student.setClassId(classId);
                student.setStatus(1);
                
                // 创建学生
                createStudent(student);
                successCount++;
                
            } catch (Exception e) {
                errors.add("第" + (i + 1) + "行：" + e.getMessage());
                failCount++;
            }
        }
        
        workbook.close();
        
        // 返回结果信息
        StringBuilder result = new StringBuilder();
        result.append("导入完成！成功：").append(successCount).append("条，失败：").append(failCount).append("条");
        if (!errors.isEmpty()) {
            result.append("\n失败详情：");
            for (String error : errors) {
                result.append("\n-").append(error);
            }
        }
        
        return result.toString();
    }
    
    /**
     * 获取单元格的值
     */
    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
    
    /**
     * 下载导入模板
     */
    public void downloadTemplate(HttpServletResponse response) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("学生导入模板");
        
        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {"学号*", "姓名*", "性别*", "班级名称", "院系名称", "手机号", "邮箱"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            
            // 设置标题样式
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            cell.setCellStyle(style);
        }
        
        // 添加示例数据
        Row exampleRow = sheet.createRow(1);
        exampleRow.createCell(0).setCellValue("2025001");
        exampleRow.createCell(1).setCellValue("张三");
        exampleRow.createCell(2).setCellValue("男");
        exampleRow.createCell(3).setCellValue("计算机2021级1班");
        exampleRow.createCell(4).setCellValue("计算机学院");
        exampleRow.createCell(5).setCellValue("13800138001");
        exampleRow.createCell(6).setCellValue("2025001@example.com");
        
        // 设置列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 20 * 256);
        }
        
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("学生导入模板.xlsx", "UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        
        // 输出文件
        OutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
        workbook.close();
    }
}
