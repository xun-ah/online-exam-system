# 在线考试系统

基于 Spring Boot + Vue3 的在线考试系统，支持管理员、教师、学生三种角色。

## 项目介绍

本系统是一个功能完善的在线考试平台，包含题库管理、试卷管理、在线考试、自动阅卷、成绩统计等功能模块，适用于学校教学管理场景。

## 技术栈

### 后端技术
- **Spring Boot 2.7.14** - Java 后端框架
- **MyBatis** - 持久层框架
- **MySQL 8.0** - 关系型数据库
- **Druid** - 数据库连接池
- **JWT** - 身份认证
- **Hutool** - Java 工具库
- **Apache POI** - Word 题目导入

### 前端技术
- **Vue 3** - 渐进式 JavaScript 框架
- **Vite** - 前端构建工具
- **Element Plus** - UI 组件库
- **Vue Router** - 路由管理
- **Pinia** - 状态管理
- **Axios** - HTTP 客户端

## 功能模块

### 管理员端
- 学生管理（增删改查、批量导入）
- 教师管理
- 班级管理
- 院系管理
- 科目管理
- 考试监控
- 数据统计与成绩分析
- 系统日志管理

### 教师端
- 题库管理（支持 Word 批量导入题目）
- 试卷管理（手动组卷）
- 考试发布
- 成绩批改（主观题）
- 成绩分析
- 所教学生管理

### 学生端
- 在线考试（实时计时、自动保存）
- 成绩查询
- 考试记录
- 错题本
- 个人中心

## 快速开始

### 环境要求
- JDK 8+
- Node.js 16+
- MySQL 8.0
- Maven 3.6+

### 后端部署

1. 进入后端目录
```bash
cd exam-system-backend
```

2. 创建数据库并导入数据
```bash
mysql -u root -p < src/main/resources/sql/init.sql
```

3. 修改数据库配置
编辑 `src/main/resources/application.yml`，修改数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/exam_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
```

4. 启动后端服务
```bash
mvn spring-boot:run
```
项目将在 http://localhost:8080/api 启动

### 前端部署

1. 进入前端目录
```bash
cd exam-admin-frontend
```

2. 安装依赖
```bash
npm install
```

3. 启动前端服务
```bash
npm run dev
```
前端将在 http://localhost:5173 启动

## 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | 123456 |
| 教师 | teacher1 | 123456 |
| 学生 | student1 | 123456 |

## 项目结构

```
在线考试系统/
├── exam-system-backend/          # 后端项目
│   ├── src/main/java/com/exam/
│   │   ├── controller/           # 控制器
│   │   ├── service/              # 业务层
│   │   ├── mapper/               # 数据访问层
│   │   ├── entity/               # 实体类
│   │   └── config/               # 配置类
│   └── src/main/resources/
│       ├── mapper/               # MyBatis 映射文件
│       ├── sql/                  # SQL 脚本
│       └── application.yml       # 配置文件
│
├── exam-admin-frontend/          # 前端项目
│   ├── src/
│   │   ├── api/                  # API 接口
│   │   ├── views/                # 页面组件
│   │   ├── router/               # 路由配置
│   │   ├── store/                # 状态管理
│   │   └── utils/                # 工具类
│   └── package.json
│
└── 20252160A0926张玉强/          # 系统截图
```

## 项目特点

1. **Word 批量导入题目** - 支持通过 Word 文档批量导入题目，提高效率
2. **同班多科支持** - 一个教师可以在同一班级教授多个科目
3. **实时考试监控** - 管理员可实时查看考试情况
4. **自动阅卷** - 客观题自动批改，主观题手动评分
5. **错题本功能** - 学生可查看错题并复习
6. **成绩统计分析** - 多维度成绩数据分析

## 注意事项

1. 本项目使用原生 MyBatis，不是 MyBatis Plus
2. 所有 Mapper 需要手动编写 XML 映射文件
3. 密码使用 BCrypt 加密存储
4. JWT Token 有效期为 24 小时
5. 支持逻辑删除（deleted 字段）

## 作者

张玉强 - 20252160A0926

## 许可证

本项目仅供学习交流使用。
