# 在线考试系统后端

## 技术栈
- Spring Boot 2.7.14
- MyBatis（非Plus）
- MySQL 8.0
- Druid连接池
- JWT认证
- Hutool工具类

## 数据库配置

### 1. 创建数据库
执行 `src/main/resources/sql/init.sql` 文件中的SQL语句来创建数据库和表结构。

```bash
mysql -u root -p < src/main/resources/sql/init.sql
```

### 2. 修改数据库配置
编辑 `src/main/resources/application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/exam_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
```

## 默认账号

### 管理员
- 用户名：admin
- 密码：123456
- 角色：管理员

### 教师
- 用户名：teacher1
- 密码：123456
- 角色：教师

### 学生
- 用户名：student1
- 密码：123456
- 角色：学生

## 运行项目

```bash
mvn spring-boot:run
```

项目将在 http://localhost:8080/api 启动

## API接口

### 认证接口
- POST /api/auth/login - 登录
- GET /api/auth/info - 获取用户信息
- POST /api/auth/logout - 登出

### 管理员接口
- GET /api/admin/students - 获取学生列表
- POST /api/admin/students - 创建学生
- PUT /api/admin/students/{id} - 更新学生
- DELETE /api/admin/students/{id} - 删除学生

### 教师接口
- GET /api/teacher/questions - 获取题目列表
- POST /api/teacher/questions - 创建题目
- GET /api/teacher/papers - 获取试卷列表
- POST /api/teacher/papers - 创建试卷

### 学生接口
- GET /api/student/exams/pending - 获取待考考试列表
- POST /api/student/exams/{id}/start - 开始考试
- POST /api/student/exams/{id}/submit - 交卷

## 项目结构

```
exam-system-backend/
├── src/main/java/com/exam/
│   ├── common/          # 通用类
│   ├── config/          # 配置类
│   ├── controller/      # 控制器
│   ├── entity/          # 实体类
│   ├── interceptor/     # 拦截器
│   ├── mapper/          # Mapper接口
│   ├── service/         # 服务层
│   └── util/            # 工具类
└── src/main/resources/
    ├── mapper/          # MyBatis XML映射文件
    ├── sql/             # SQL脚本
    └── application.yml  # 配置文件
```

## 注意事项

1. 本项目使用原生MyBatis，不是MyBatis Plus
2. 所有Mapper需要手动编写XML映射文件
3. 密码使用BCrypt加密存储
4. JWT Token有效期为24小时
5. 支持逻辑删除（deleted字段）
