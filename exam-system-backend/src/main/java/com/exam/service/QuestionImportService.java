package com.exam.service;

import com.exam.entity.Question;
import com.exam.mapper.QuestionMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuestionImportService {

    @Autowired
    private QuestionMapper questionMapper;
    
    @Autowired
    private QuestionService questionService;

    /**
     * 批量导入题目（根据文件类型自动选择Word或Excel）
     */
    public int importQuestions(MultipartFile file, Long teacherId, String subject) throws Exception {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new Exception("文件名不能为空");
        }
        
        if (fileName.endsWith(".docx")) {
            return importQuestionsFromWord(file, teacherId, subject);
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return importQuestionsFromExcel(file, teacherId, subject);
        } else {
            throw new Exception("仅支持.docx、.xlsx和.xls格式的文件");
        }
    }
    
    /**
     * 批量导出题目为Excel
     */
    public void exportQuestions(Long teacherId, Integer type, Integer difficulty, 
                               String keyword, String subject, HttpServletResponse response) throws Exception {
        // 获取题目列表（不分页，获取所有符合条件的题目）
        List<Question> questions = questionService.getQuestionsByCondition(
            teacherId, type, difficulty, keyword, subject);
        
        if (questions == null || questions.isEmpty()) {
            throw new Exception("没有可导出的题目");
        }
        
        // 创建Excel工作簿
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("题目导出");
        
        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {"题型", "难度", "题目内容", "选项A", "选项B", "选项C", "选项D", "答案", "解析", "科目", "分值"};
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
        
        // 填充数据
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            Row row = sheet.createRow(i + 1);
            
            // 题型
            row.createCell(0).setCellValue(getQuestionTypeText(q.getType()));
            // 难度
            row.createCell(1).setCellValue(getDifficultyText(q.getDifficulty()));
            // 题目内容
            row.createCell(2).setCellValue(q.getContent() != null ? q.getContent() : "");
            
            // 解析选项
            String[] options = parseOptions(q.getOptions());
            row.createCell(3).setCellValue(options[0]); // A
            row.createCell(4).setCellValue(options[1]); // B
            row.createCell(5).setCellValue(options[2]); // C
            row.createCell(6).setCellValue(options[3]); // D
            
            // 答案
            row.createCell(7).setCellValue(q.getAnswer() != null ? q.getAnswer() : "");
            // 解析
            row.createCell(8).setCellValue(q.getAnalysis() != null ? q.getAnalysis() : "");
            // 科目
            row.createCell(9).setCellValue(q.getSubject() != null ? q.getSubject() : "");
            // 分值
            row.createCell(10).setCellValue(q.getScore() != null ? q.getScore().doubleValue() : 0);
        }
        
        // 设置列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 4000);
        }
        sheet.setColumnWidth(2, 8000); // 题目内容列更宽
        
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("题目导出_" + System.currentTimeMillis(), "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        
        // 写入响应
        try (OutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
            outputStream.flush();
        } finally {
            workbook.close();
        }
    }
    
    /**
     * 获取题型文本
     */
    private String getQuestionTypeText(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "单选题";
            case 2: return "多选题";
            case 3: return "判断题";
            case 4: return "填空题";
            case 5: return "简答题";
            case 6: return "编程题";
            default: return "未知";
        }
    }
    
    /**
     * 获取难度文本
     */
    private String getDifficultyText(Integer difficulty) {
        if (difficulty == null) return "中等";
        switch (difficulty) {
            case 1: return "简单";
            case 2: return "中等";
            case 3: return "困难";
            default: return "中等";
        }
    }
    
    /**
     * 解析选项JSON为数组
     */
    private String[] parseOptions(String optionsJson) {
        String[] options = {"", "", "", ""};
        if (optionsJson == null || optionsJson.trim().isEmpty()) {
            return options;
        }
        
        try {
            // 简单解析JSON格式的选项
            if (optionsJson.contains("\"A\":")) {
                int start = optionsJson.indexOf("\"A\":\"") + 5;
                int end = optionsJson.indexOf("\"", start);
                if (end > start) options[0] = optionsJson.substring(start, end);
            }
            if (optionsJson.contains("\"B\":")) {
                int start = optionsJson.indexOf("\"B\":\"") + 5;
                int end = optionsJson.indexOf("\"", start);
                if (end > start) options[1] = optionsJson.substring(start, end);
            }
            if (optionsJson.contains("\"C\":")) {
                int start = optionsJson.indexOf("\"C\":\"") + 5;
                int end = optionsJson.indexOf("\"", start);
                if (end > start) options[2] = optionsJson.substring(start, end);
            }
            if (optionsJson.contains("\"D\":")) {
                int start = optionsJson.indexOf("\"D\":\"") + 5;
                int end = optionsJson.indexOf("\"", start);
                if (end > start) options[3] = optionsJson.substring(start, end);
            }
        } catch (Exception e) {
            System.err.println("解析选项失败: " + e.getMessage());
        }
        
        return options;
    }

    /**
     * 从Excel文件批量导入题目
     * 格式：题型 | 题目内容 | 选项A | 选项B | 选项C | 选项D | 答案 | 解析
     */
    public int importQuestionsFromExcel(MultipartFile file, Long teacherId, String subject) throws Exception {
        List<Question> questions = new ArrayList<>();
        
        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook;
            String fileName = file.getOriginalFilename();
            
            if (fileName.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(inputStream);
            } else {
                workbook = new HSSFWorkbook(inputStream);
            }
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // 从第二行开始读取（第一行是标题）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    Question question = parseExcelRow(row, teacherId, subject);
                    if (question != null) {
                        questions.add(question);
                    }
                } catch (Exception e) {
                    System.err.println("解析第" + (i + 1) + "行失败: " + e.getMessage());
                }
            }
            
            workbook.close();
        }
        
        // 批量插入数据库
        int successCount = 0;
        for (Question question : questions) {
            try {
                if (question.getQuestionNo() == null || question.getQuestionNo().isEmpty()) {
                    question.setQuestionNo(generateQuestionNo(teacherId));
                }
                questionMapper.insert(question);
                successCount++;
            } catch (Exception e) {
                System.err.println("导入题目失败: " + question.getContent() + ", 错误: " + e.getMessage());
            }
        }
        
        return successCount;
    }

    /**
     * 解析Excel行数据
     * 格式：题型 | 难度 | 题目内容 | 选项A | 选项B | 选项C | 选项D | 答案 | 解析
     */
    private Question parseExcelRow(Row row, Long teacherId, String subject) {
        try {
            String typeStr = getCellValue(row.getCell(0));
            String difficultyStr = getCellValue(row.getCell(1));
            String content = getCellValue(row.getCell(2));
            String optionA = getCellValue(row.getCell(3));
            String optionB = getCellValue(row.getCell(4));
            String optionC = getCellValue(row.getCell(5));
            String optionD = getCellValue(row.getCell(6));
            String answer = getCellValue(row.getCell(7));
            String analysis = getCellValue(row.getCell(8));
            
            if (content == null || content.trim().isEmpty()) {
                return null;
            }
            
            // 解析题型
            Integer type = parseQuestionType(typeStr);
            if (type == null) {
                type = 1; // 默认单选题
            }
            
            // 解析难度
            Integer difficulty = parseDifficulty(difficultyStr);
            
            Question question = new Question();
            question.setType(type);
            question.setTeacherId(teacherId);
            question.setSubject(subject);
            question.setDifficulty(difficulty);
            question.setScore(new BigDecimal("5"));
            question.setContent(content.trim());
            question.setAnswer(answer != null ? answer.trim() : "");
            question.setAnalysis(analysis != null ? analysis.trim() : "");
            
            // 构建选项JSON
            if (optionA != null || optionB != null || optionC != null || optionD != null) {
                StringBuilder optionsJson = new StringBuilder("{");
                if (optionA != null) optionsJson.append("\"A\":\"").append(optionA.trim()).append("\"");
                if (optionB != null) {
                    if (optionsJson.length() > 1) optionsJson.append(",");
                    optionsJson.append("\"B\":\"").append(optionB.trim()).append("\"");
                }
                if (optionC != null) {
                    if (optionsJson.length() > 1) optionsJson.append(",");
                    optionsJson.append("\"C\":\"").append(optionC.trim()).append("\"");
                }
                if (optionD != null) {
                    if (optionsJson.length() > 1) optionsJson.append(",");
                    optionsJson.append("\"D\":\"").append(optionD.trim()).append("\"");
                }
                optionsJson.append("}");
                question.setOptions(optionsJson.toString());
            }
            
            return question;
        } catch (Exception e) {
            System.err.println("解析行数据失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 解析题型字符串
     */
    private Integer parseQuestionType(String typeStr) {
        if (typeStr == null) return null;
        
        typeStr = typeStr.trim();
        if (typeStr.contains("单选") || typeStr.equals("1")) return 1;
        if (typeStr.contains("多选") || typeStr.equals("2")) return 2;
        if (typeStr.contains("判断") || typeStr.equals("3")) return 3;
        if (typeStr.contains("填空") || typeStr.equals("4")) return 4;
        if (typeStr.contains("简答") || typeStr.equals("5")) return 5;
        
        return null;
    }

    /**
     * 解析难度字符串
     */
    private Integer parseDifficulty(String difficultyStr) {
        if (difficultyStr == null || difficultyStr.trim().isEmpty()) {
            return 2; // 默认中等
        }
        
        difficultyStr = difficultyStr.trim();
        if (difficultyStr.contains("简单") || difficultyStr.contains("易") || difficultyStr.equals("1")) return 1;
        if (difficultyStr.contains("中等") || difficultyStr.contains("中") || difficultyStr.equals("2")) return 2;
        if (difficultyStr.contains("困难") || difficultyStr.contains("难") || difficultyStr.equals("3")) return 3;
        
        return 2; // 默认中等
    }

    /**
     * 获取单元格值
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
     * 从Word文件批量导入题目
     * 支持的格式：
     * 一、单选题
     * 1. 题目内容
     * A. 选项A
     * B. 选项B
     * C. 选项C
     * D. 选项D
     * 答案：A
     * 解析：解析内容（可选）
     */
    public int importQuestionsFromWord(MultipartFile file, Long teacherId, String subject) throws Exception {
        List<Question> questions = new ArrayList<>();
        
        try (InputStream inputStream = file.getInputStream()) {
            XWPFDocument document = new XWPFDocument(inputStream);
            
            // 提取所有文本
            StringBuilder fullText = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                fullText.append(paragraph.getText()).append("\n");
            }
            
            String text = fullText.toString();
            questions = parseQuestions(text, teacherId, subject);
        }
        
        // 批量插入数据库
        int successCount = 0;
        for (Question question : questions) {
            try {
                // 生成题目编号
                if (question.getQuestionNo() == null || question.getQuestionNo().isEmpty()) {
                    question.setQuestionNo(generateQuestionNo(teacherId));
                }
                questionMapper.insert(question);
                successCount++;
            } catch (Exception e) {
                // 记录错误但继续导入其他题目
                System.err.println("导入题目失败: " + question.getContent() + ", 错误: " + e.getMessage());
            }
        }
        
        return successCount;
    }

    /**
     * 解析题目文本
     */
    private List<Question> parseQuestions(String text, Long teacherId, String subject) {
        List<Question> questions = new ArrayList<>();
        
        // 按题型分割（一、单选题 / 二、多选题 等）
        String[] sections = text.split("([一二三四五六七八九十]+)、");
        
        for (String section : sections) {
            if (section.trim().isEmpty()) continue;
            
            // 提取题型
            Integer type = extractQuestionType(section);
            if (type == null) continue;
            
            // 分割各个题目
            String[] questionBlocks = section.split("\n\\d+\\.");
            
            for (int i = 1; i < questionBlocks.length; i++) { // 从1开始，跳过题型标题
                String block = questionBlocks[i].trim();
                if (block.isEmpty()) continue;
                
                Question question = parseSingleQuestion(block, type, teacherId, subject);
                if (question != null) {
                    questions.add(question);
                }
            }
        }
        
        return questions;
    }

    /**
     * 解析单个题目
     */
    private Question parseSingleQuestion(String block, Integer type, Long teacherId, String subject) {
        try {
            Question question = new Question();
            question.setType(type);
            question.setTeacherId(teacherId);
            question.setSubject(subject); // 设置科目
            question.setDifficulty(2); // 默认中等难度
            question.setScore(new BigDecimal("5")); // 默认5分
            
            String[] lines = block.split("\n");
            StringBuilder contentBuilder = new StringBuilder();
            StringBuilder answerBuilder = new StringBuilder();
            StringBuilder analysisBuilder = new StringBuilder();
            StringBuilder optionsBuilder = new StringBuilder();
            
            boolean inOptions = false;
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // 检查是否是答案行
                if (line.matches("答案[：:].*")) {
                    String answer = line.replaceAll("答案[：:]", "").trim();
                    answerBuilder.append(answer);
                    continue;
                }
                
                // 检查是否是解析行
                if (line.matches("(解析|答案解析)[：:].*")) {
                    String analysis = line.replaceAll("(解析|答案解析)[：:]", "").trim();
                    analysisBuilder.append(analysis);
                    continue;
                }
                
                // 检查是否是选项
                if (line.matches("[A-D][.．、].*")) {
                    String option = line.replaceFirst("[A-D][.．、]", "").trim();
                    char optionChar = line.charAt(0);
                    if (optionsBuilder.length() > 0) {
                        optionsBuilder.append(",");
                    }
                    optionsBuilder.append(optionChar).append(":").append(option);
                    inOptions = true;
                    continue;
                }
                
                // 其他行作为题目内容
                if (!inOptions || !line.matches("^[A-D].*")) {
                    if (contentBuilder.length() > 0) {
                        contentBuilder.append("\n");
                    }
                    contentBuilder.append(line);
                    inOptions = false;
                }
            }
            
            question.setContent(contentBuilder.toString());
            question.setAnswer(answerBuilder.toString());
            question.setAnalysis(analysisBuilder.toString());
            
            // 如果有选项，转换为JSON格式
            if (optionsBuilder.length() > 0) {
                question.setOptions("{" + optionsBuilder + "}");
            }
            
            return question;
        } catch (Exception e) {
            System.err.println("解析题目失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从文本中提取题型
     */
    private Integer extractQuestionType(String section) {
        String firstLine = section.split("\n")[0].trim();
        
        if (firstLine.contains("单选题")) return 1;
        if (firstLine.contains("多选题")) return 2;
        if (firstLine.contains("判断题")) return 3;
        if (firstLine.contains("填空题")) return 4;
        if (firstLine.contains("简答题")) return 5;
        
        return null;
    }

    /**
     * 生成题目编号
     */
    private String generateQuestionNo(Long teacherId) {
        long timestamp = System.currentTimeMillis();
        String timestampSuffix = String.valueOf(timestamp).substring(7);
        return "Q" + teacherId + timestampSuffix;
    }
}
