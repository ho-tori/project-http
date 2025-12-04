# Project HTTP — 基于 Java Socket 的简易 HTTP 客户端/服务器

本项目实现了一个“纯 Socket”的 HTTP 服务器与客户端（含命令行与 GUI），演示请求解析、路由与处理器、静态资源服务、缓存协商（304）、长连接、重定向、MIME 类型、以及简单的注册/登录与文件上传等功能。

- 语言与依赖：Java 8+，仅使用标准库与 `org.json`（解析用户数据）。
- IO 模型：BIO（`ServerSocket` + 多线程，每连接一线程）。
- 不使用 Netty/Undertow/Tomcat 等框架，严格基于 Java Socket API。

---

## 目录结构

```
src/
  main/java/com/http/
    client/
      HttpClient.java            # 命令行客户端（支持重定向处理、二进制保存）
    common/
      HttpRequest.java           # 请求解析（流式、支持 Content-Length）
      HttpResponse.java          # 响应构造（toBytes）
      HttpStatus.java            # 状态码常量与短语
      MimeType.java              # MIME 映射与文本类型判断
    gui/
      ServerGUI.java             # 服务端 GUI（启动/停止/打开浏览器/日志）
      ClientGUI.java             # 客户端 GUI（方法/URL/Headers/Body/响应展示）
    server/
      HttpServer.java            # 服务器主类（端口监听、可停止）
      ConnectionHandler.java     # 连接处理（长连接、异常->500）
      router/Router.java         # 路由（重定向示例、API、静态文件）
      handler/
        StaticFileHandler.java   # 静态文件/HEAD/304/404/405/500
        LoginHandler.java        # 登录（POST JSON）
        RegisterHandler.java     # 注册（POST JSON）
        FileUploadHandler.java   # 上传（自动识别扩展名）
      auth/
        UserManager.java         # 内存用户表（初始化自 JSON 文件）
        users.json               # 初始用户数据
  main/java/web/                 # 静态资源根目录（index.html/new-page.html/404.html/...）
```

---

## 快速开始（Windows PowerShell）

推荐使用 VS Code 的 Java 扩展直接运行 main 方法；或使用 Maven 编译运行。

### 方式 A：VS Code 一键运行（推荐）
- 服务端 GUI：打开 `src/main/java/com/http/gui/ServerGUI.java`，运行 `main`。
- 客户端 GUI：打开 `src/main/java/com/http/gui/ClientGUI.java`，运行 `main`。

### 方式 B：Maven 编译与运行
```pwsh
# 在项目根目录
mvn -q clean package

# 运行服务端 GUI（注意 classpath 需包含依赖）
# 替换为你本机 Maven 仓库 org.json 的实际路径
$JSON="${env:USERPROFILE}\.m2\repository\org\json\json\20230227\json-20230227.jar"
java -cp "target/classes;$JSON" com.http.gui.ServerGUI

# 新开一个终端，运行客户端 GUI
java -cp "target/classes;$JSON" com.http.gui.ClientGUI
```

> 说明：你也可以用 Maven Exec 插件运行：
> `mvn -q org.codehaus.mojo:exec-maven-plugin:3.5.0:java -Dexec.mainClass=com.http.gui.ServerGUI`

### 方式 C：仅编译后运行（不使用 Maven 依赖管理，不推荐）
若手动 `javac` 编译，需自行把 `org.json` 的 jar 放入 `-cp`。建议优先使用 Maven。

---

## 在浏览器中访问
- 启动服务端后（默认端口见 `HttpServer.java` 或 GUI 中设置，示例 6175）：
  - 首页：`http://localhost:6175/`
  - 静态文件：`/index.html`, `/new-page.html`, `/test.txt`
  - 重定向测试：`/old-page` (301 -> `/new-page.html`), `/redirect-test` (302 -> `/new-page.html`)

---

## 客户端用法

### 命令行客户端（`HttpClient`）
```pwsh
# 示例：GET
curl http://localhost:6175/  # 也可在客户端 CLI 内输入 GET / 

# 在客户端 CLI 内的命令：
GET /index.html
POST /api/upload ./somefile.png   # 上传文件
REGISTER alice 123456             # 注册
LOGIN alice 123456                # 登录
QUIT
```
- 支持自动处理 301/302 重定向、识别 304 未修改。
- 对二进制响应会提示大小，并在 GET 后按内容类型保存到 `downloads/`。

### GUI 客户端（`ClientGUI`）
- 可设置方法（GET/POST/PUT/DELETE/HEAD）、URL、Headers、以及请求体（支持从文件载入）。
- 响应页签展示状态行、所有响应头、以及文本/二进制提示。

---

## API 与路由

- 路由（`Router.java`）
  - `GET /old-page` → 301 重定向至 `/new-page.html`
  - `GET /redirect-test` → 302 重定向至 `/new-page.html`
  - `POST /api/register` 或 `/register` → 注册
  - `POST /api/login` 或 `/login` → 登录
  - `POST /api/upload` 或 `/upload` → 上传
  - 其他路径默认走静态资源处理（`StaticFileHandler`）

- 注册（`RegisterHandler`）
  - Method：`POST`
  - Body：`{"username":"...","password":"..."}`
  - 成功：`200 OK`，失败：`400 Bad Request`

- 登录（`LoginHandler`）
  - Method：`POST`
  - Body：`{"username":"...","password":"..."}`
  - 成功：`200 OK`，失败：`404 Not Found`（用户名或密码不匹配）

- 上传（`FileUploadHandler`）
  - Method：`POST`
  - Body：原始文件二进制（不使用 multipart）
  - 自动识别扩展名（magic number / Content-Type / 文本启发式），保存至 `src/main/java/web/uploads/`

- 静态资源（`StaticFileHandler`）
  - 支持 `GET/HEAD`，默认目录 `src/main/java/web/`
  - 目录 `/` → `index.html`
  - 路径穿越防护（拒绝包含 `..`）
  - 缓存：支持 `Last-Modified` 与 `If-Modified-Since`，命中返回 `304 Not Modified`

---

## 需求对照与实现说明

1) 纯 Socket 实现（不使用 Netty 等）
- 服务器：`java.net.ServerSocket` + `Socket`，代码参见 `HttpServer.java`、`ConnectionHandler.java`
- 客户端：`Socket` 写请求、读响应，参见 `HttpClient.java`

2) IO 模型
- BIO：每个连接一个线程（`new Thread(new ConnectionHandler(...)).start()`）

3) HTTP 客户端
- 发送请求与呈现响应：命令行（`HttpClient`）与 GUI（`ClientGUI`）均已实现
- 重定向与缓存：
  - 301/302：`HttpClient.handleRedirect` 支持跟随，GUI 直接展示 Location 供用户选择
  - 304：命令行和 GUI 均可看到 `304 Not Modified`，静态资源按 `If-Modified-Since` 命中

4) HTTP 服务器
- 方法：`GET/POST`（静态/登录/注册/上传），`HEAD`（静态）
- 状态码：
  - `200 OK`（成功）
  - `301/302`（路由演示重定向）
  - `304 Not Modified`（静态资源缓存命中）
  - `404 Not Found`（文件缺失/非法路径）
  - `405 Method Not Allowed`（不支持的方法，如对静态资源的非 GET/HEAD）
  - `500 Internal Server Error`（异常兜底，`ConnectionHandler#create500ErrorResponse`）
- 长连接：
  - `ConnectionHandler` 循环处理同一 `Socket` 上的多请求
  - `HTTP/1.1` 默认 keep-alive；对 `HTTP/1.0` 或 `Connection: close` 改为短连接

5) MIME 类型
- `text/html`、`text/plain`、`image/png`、`image/jpeg` 等
- 客户端依据 `MimeType.isTextType` 判断文本/二进制，下载二进制

6) 注册/登录（内存）
- `UserManager` 使用 `ConcurrentHashMap` 存储；初始化可从 `users.json` 读取

---

## 设计亮点与加分点

- 长连接支持：同一 TCP 连接上循环处理多个 HTTP 请求，符合 HTTP/1.1 默认行为。
- 304 缓存协商：使用 `Last-Modified / If-Modified-Since`，节省带宽与延迟。
- HEAD 支持：静态资源对 `HEAD` 仅返回头部，符合规范。
- 路由与重定向：在 `Router` 中集中管理，演示 301/302。
- MIME 与二进制下载：服务端正确标注类型，客户端区分并保存二进制。
- 文件上传自动识别：优先基于 Magic Number 识别图片类型，其次 Content-Type，最后文本启发式。
- 安全性考虑：静态资源路径穿越防护（阻止 `..`）、限定 `webRoot` 前缀、上传目录隔离。
- GUI 友好：
  - 服务器 GUI：一键启动/停止、日志重定向、打开浏览器
  - 客户端 GUI：方法、URL、Headers、Body、响应页签，适合教学与调试

---

## 调试与测试示例

```pwsh
# 启动服务端（示例端口 6175）后：

# 1) 静态页面
curl -v http://localhost:6175/

# 2) 重定向（显示 Location）
curl -v http://localhost:6175/old-page

# 3) 条件请求缓存（If-Modified-Since 用上次响应的 Last-Modified 值）
$lm = (curl -sI http://localhost:6175/index.html | Select-String 'Last-Modified').ToString().Split(': ',2)[1]
curl -v http://localhost:6175/index.html -H "If-Modified-Since: $lm"

# 4) 注册/登录
curl -v -X POST http://localhost:6175/api/register -H "Content-Type: application/json" -d '{"username":"alice","password":"123456"}'
curl -v -X POST http://localhost:6175/api/login    -H "Content-Type: application/json" -d '{"username":"alice","password":"123456"}'

# 5) 上传（发送原始文件内容，不是 multipart）
# PowerShell 可用 --data-binary，或使用 curl 原生命令行
curl -v -X POST http://localhost:6175/api/upload --data-binary @"src/main/java/web/test.txt"
```

---

## 实现细节速览

- 请求解析（`HttpRequest(InputStream)`）
  - 不依赖 `BufferedReader` 的 `readLine()`，以字节流精确截取 `\r\n\r\n` 头部结束，再按 `Content-Length` 读取 body。
- 响应构造（`HttpResponse.toBytes()`）
  - 手工拼装状态行、头部与 body，确保字节级正确性。
- 连接处理（`ConnectionHandler`）
  - try/catch 区分 `SocketTimeout`、`EOF`，异常兜底返回 500。
- 静态文件（`StaticFileHandler`）
  - 统一的 `buildNotFound/MethodNotAllowed/InternalError` 构造器。
  - `Last-Modified` 使用 `RFC_1123`；`HEAD` 不写 body。

---

## 潜在改进方向

- 完善重定向 Location 绝对化（含协议/Host），避免部分客户端兼容性问题。
- 客户端对二进制响应的解析应采用字节流（避免字符流破坏），并支持分块传输（chunked）。
- Keep-Alive 与线程池：改为线程池或使用 NIO，提升并发能力。
- 更丰富的缓存策略（ETag/If-None-Match）。
- 更安全的鉴权（会话/Token/密码哈希存储）。
- 上传支持 multipart/form-data 与表单字段。

---

## 许可证
仅用于课程学习与演示。