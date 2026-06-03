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

项目将在 http://localhost:8088/api 启动

## 常用运维命令

### 启动后端服务
```bash
cd exam-system-backend
mvn spring-boot:run
```

### 终止后端服务（Windows）

**方法1：查找并终止占用8088端口的进程**
```powershell
# 1. 查找占用8088端口的进程PID
netstat -ano | findstr :8088

# 2. 终止进程（将<PID>替换为实际进程号）
taskkill /F /PID <PID>
```

**方法2：直接终止Java进程**
```powershell
# 查找所有Java进程
tasklist | findstr java

# 终止指定PID的Java进程
taskkill /F /PID <PID>
```

### 端口占用检查与处理

**检查端口是否被占用**
```powershell
netstat -ano | findstr :8088
```

输出示例：
```
TCP    0.0.0.0:8088           0.0.0.0:0              LISTENING       12345
TCP    [::]:8088              [::]:0                 LISTENING       12345
```
最后一列 `12345` 即为占用端口的进程PID。

**强制终止占用端口的进程**
```powershell
taskkill /F /PID 12345
```

**一次性检查并终止（推荐）**
```powershell
# 如果端口被占用，自动终止
for /f "tokens=5" %a in ('netstat -ano ^| findstr :8088') do @taskkill /F /PID %a
```

### 重新编译并启动
```bash
# 清理并重新编译
mvn clean compile -DskipTests

# 启动服务
mvn spring-boot:run
```

### 常见问题

**问题1：端口8088已被占用**
```
错误信息：Port 8088 was already in use
解决方法：按照上述端口检查步骤，找到并终止占用进程
```

**问题2：代码修改后未生效**
```bash
# 需要清除IDEA缓存并重新编译
mvn clean compile -DskipTests
mvn spring-boot:run
```

**问题3：前端缓存问题**
```bash
# 清除浏览器缓存或使用 Ctrl+F5 强制刷新
# 或重启前端开发服务器
cd exam-admin-frontend
npm run dev
```

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
