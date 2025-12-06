## 一、实验名称与主题

**主题 1：基于 Java Socket API 搭建简单的 HTTP 客户端和服务器端程序**

本实验在不依赖 Netty 等高级网络框架的前提下，使用 **Java 原生 Socket API** 从零实现一个简易的 HTTP 服务器与 HTTP 客户端（命令行版 + GUI 版），用于加深对 HTTP 报文结构、状态码、长连接、静态资源与认证等内容的理解。

---

## 二、实验环境与项目结构

- **开发语言**：Java 8  
- **构建工具**：Maven  
- **主要依赖**：`org.json:json:20230227`（仅用于解析/构造用户认证相关 JSON 数据）  
- **操作系统**：Windows 10  

项目主要目录结构（部分）：

- `src/main/java/com/http/server/HttpServer.java`：HTTP 服务器入口，负责监听端口、接受 Socket 连接。  
- `src/main/java/com/http/server/ConnectionHandler.java`：单个客户端连接处理线程，实现请求解析、路由分发与长连接控制。  
- `src/main/java/com/http/server/router/Router.java`：路由分发中心，根据 URI 和方法选择对应 Handler。  
- `src/main/java/com/http/server/handler/StaticFileHandler.java`：静态资源处理（HTML、文本、图片等），支持 200/404/500/304 等状态码。  
- `src/main/java/com/http/server/handler/LoginHandler.java`：登录接口处理。  
- `src/main/java/com/http/server/handler/RegisterHandler.java`：注册接口处理。  
- `src/main/java/com/http/server/handler/FileUploadHandler.java`：简易文件上传处理。  
- `src/main/java/com/http/server/auth/UserManager.java`：用户注册/登录的内存管理，启动时从 `users.json` 预加载部分账号。  
- `src/main/java/com/http/common/HttpRequest.java`：HTTP 请求报文的解析与封装。  
- `src/main/java/com/http/common/HttpResponse.java`：HTTP 响应报文的封装与序列化。  
- `src/main/java/com/http/common/HttpStatus.java`：HTTP 状态码常量及对应短语。  
- `src/main/java/com/http/common/MimeType.java`：MIME 类型映射与文本类型判断。  
- `src/main/java/com/http/client/HttpClient.java`：命令行 HTTP 客户端，实现请求构造、发送与响应显示，并处理 301/302/304。  
- `src/main/java/com/http/client/HttpClientGUI.java`：基于 Swing 的淡紫色 GUI 客户端。  
- `src/main/java/web/*.html`：静态网页资源（首页、404 页等）。  

### 目录结构

```
src/
  main/java/com/http/
    client/
      HttpClient.java            # 命令行客户端（支持重定向处理、二进制保存）
      HttpClientGUI.java         # GUI 客户端
    common/
      HttpRequest.java           # 请求解析（流式、支持 Content-Length）
      HttpResponse.java          # 响应构造（toBytes）
      HttpStatus.java            # 状态码常量与短语
      MimeType.java              # MIME 映射与文本类型判断
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

## 三、实验要求对应关系概述

根据题目要求，项目实现与对应位置如下：

1. **完全基于 Java Socket API**  
   - 服务器端使用 `java.net.ServerSocket` 在 `HttpServer` 中监听端口，使用 `Socket` 进行读写通信；代码中未使用 Netty 等高层框架。  
   - 客户端同样使用 `Socket` 在 `HttpClient.sendRequest` 中与服务器通信。

2. **IO 模型不限（本项目使用 BIO）**  
   - 服务器端采用 **阻塞 IO（BIO）**：`ServerSocket.accept()` + 每个连接一个 `ConnectionHandler` 线程，简洁直观。  

3. **基础 HTTP 请求 / 响应功能**  
   1. 客户端可以发送请求报文、展示响应报文  
      - `HttpClient` 中手工拼接请求行、请求头与请求体，通过 `OutputStream` 发送；  
      - 使用 `BufferedReader` 读取响应头和内容，解析为 `HttpResponse` 对象并打印到控制台；  
      - `HttpClientGUI` 在图形界面上提供 Host/Port/URI/请求体等输入，并在下方文本区展示完整的响应头和响应体。  
   2. 客户端对 301、302、304 的状态码处理  
      - `HttpClient.handleRedirect` 中循环检查响应状态码：当为 301（Moved Permanently）或 302（Found）时，根据 `Location` 头自动重新发起 GET 请求；  
      - 当状态码为 304（Not Modified）时，客户端打印提示“资源未修改 (304)”，不再请求新的内容。  
   3. 服务器端支持 GET 和 POST  
      - 在 `Router` 中，针对 `/api/login`、`/api/register`、`/api/upload` 等路径，只接受 `POST`，由对应 Handler 处理；  
      - 其它路径默认交给 `StaticFileHandler`，主要处理 `GET` 和 `HEAD` 静态资源请求。  
   4. 服务器端支持 **200、301、302、304、404、405、500**  
      - `HttpStatus` 中定义了这些常量，`getReasonPhrase` 返回对应英文短语。  
      - 不同 Handler 中根据逻辑设置不同状态码：  
        - 200：正常返回（静态文件、登录成功、注册成功、上传成功等）；  
        - 301、302：`Router` 中 `/old-page` 和 `/redirect-test` 分别返回永久 / 临时重定向；  
        - 304：`StaticFileHandler` 中对 `If-Modified-Since / Last-Modified` 头进行比较，若资源未修改则返回 304；  
        - 404：静态文件不存在时返回 404，优先使用自定义 `404.html`；  
        - 405：不允许的方法（如对静态文件使用非 GET/HEAD，或对登录/注册接口使用非 POST）时返回 405；  
        - 500：在 `ConnectionHandler` 的异常捕获中构造 500 Internal Server Error 响应。  
   5. 服务器端实现长连接  
      - `ConnectionHandler` 中使用 `while (keepAlive)` 循环在同一 TCP 连接上处理多次 HTTP 请求：  
        - 当客户端显式指定 `Connection: close` 或使用 HTTP/1.0 时，服务器设置响应头为 `Connection: close` 并在一次请求后关闭连接；  
        - 否则默认保持 HTTP/1.1 的长连接，返回 `Connection: keep-alive` 与 `Keep-Alive: timeout=30, max=100`。  
   6. MIME 至少三种类型，包含一种非文本类型  
      - `MimeType` 中定义了多种扩展名到 MIME 的映射：`text/html`、`text/plain`、`image/png`、`image/jpeg` 等；  
      - `StaticFileHandler` 根据文件名调用 `MimeType.getMimeType` 设置 `Content-Type`，并通过 `MimeType.isTextType` 区分文本与二进制类型，在客户端展示时做不同处理。  

4. **注册、登录功能（数据可在内存中维护）**  
   - `UserManager` 内部通过一个 `ConcurrentHashMap<String, String>` 维护用户名与密码映射：  
     - 启动时可从 `users.json` 读取初始账号；  
     - `register` 方法检查用户名是否存在、密码长度是否合法，然后将新用户写入内存 Map；  
     - `login` 方法验证用户名存在且密码匹配。  
   - `RegisterHandler`：  
     - 仅接受 POST，Body 为简易 JSON：`{"username":"...","password":"..."}`；  
     - 成功返回 200 和 JSON：`{"status":"ok","message":"registered"}`；  
     - 失败（用户名已存在或不合法）返回 400。  
   - `LoginHandler`：  
     - 仅接受 POST，解析 JSON 提取用户名和密码；  
     - 成功时返回 200，Body `"Login successful!"`；  
     - 失败时返回 404 并提示 `"Invalid credentials."`。  
   - 这些接口可通过 Postman/浏览器/自写客户端发送 POST 请求进行测试，无需额外前端页面。  

---

## 四、关键模块设计与实现思路

### 4.1 服务器端整体架构

1. **启动与监听**  
   - `HttpServer` 在指定端口（如 6175）上创建 `ServerSocket`，持续调用 `accept()` 等待新的客户端连接。  
   - 每当有新的 Socket 连接到来时，创建一个 `ConnectionHandler` 线程负责该连接的整个生命周期。  

2. **连接处理与长连接**  
   - `ConnectionHandler.run()` 内部：  
     - 获取 `InputStream` / `OutputStream`；  
     - 在 `while (keepAlive)` 循环中：  
       1. 构造 `HttpRequest`，从输入流中按照 HTTP 协议读取请求行、头部与可选的请求体；  
       2. 将请求交给 `Router.route(request)`，获得 `HttpResponse`；  
       3. 根据请求头中的 `Connection` 和协议版本判断是否保持长连接；  
       4. 调用 `response.toBytes()` 将响应序列化后写入输出流；  
       5. 如需关闭连接，跳出循环并关闭 Socket。  

3. **路由与 Handler**  
   - `Router.route` 根据 `URI` 和 `Method` 实现以下路由策略：  
     - `/old-page` → 返回 301，`Location: /new-page.html`；  
     - `/redirect-test` → 返回 302，`Location: /new-page.html`；  
     - `/api/login` 或 `/login` + POST → `LoginHandler`；  
     - `/api/register` 或 `/register` + POST → `RegisterHandler`；  
     - `/api/upload` 或 `/upload` + POST → `FileUploadHandler`；  
     - 其它路径 → `StaticFileHandler` 提供静态文件服务。  

4. **静态文件与状态码**  
   - `StaticFileHandler` 负责在 `src/main/java/web` 目录下查找请求对应的资源：  
     - 将 `/` 映射到 `/index.html`；  
     - 检查路径安全性（避免 `..` 路径穿越）；  
     - 若文件存在：  
       - 读取内容、根据扩展名设置合适 MIME 类型；  
       - 支持 `GET` 与 `HEAD`；HEAD 请求只返回头部不包含 body；  
       - 计算并设置 `Last-Modified` 头，用于 304 缓存协商。  
     - 若文件不存在：  
       - 优先返回自定义的 `404.html`；  
       - 如果找不到 404 页面，则返回纯文本 `"404 Not Found"`。  

5. **304 Not Modified 的实现**  
   - 在返回静态资源前，根据目标文件的 `lastModified` 时间生成 `Last-Modified`（遵循 RFC 1123 日期格式）；  
   - 若请求头中包含 `If-Modified-Since`，且值与当前 `Last-Modified` 完全一致，则直接返回：  
     - 状态码 304；  
     - 带上 `Last-Modified` 与 `Connection`；  
     - `Content-Length: 0`，不再发送响应体。  

### 4.2 客户端设计（命令行 + GUI）

1. **命令行客户端 `HttpClient`**  
   - 通过 `Socket(host, port)` 建立连接。  
   - 按照 HTTP/1.1 协议格式组装请求行和请求头，并可选地附加请求体（例如 JSON 字符串或文件字节流）；  
   - 支持 `get(uri)` 与 `post(uri, body)` 两类封装方法；  
   - 提供命令行交互界面：  
     - `GET <uri>`：发送 GET 请求并显示响应；  
     - `POST <uri> <text|file_path>`：支持发送文本或上传文件；  
     - `REGISTER <username> <password>`：向 `/api/register` 发送 JSON；  
     - `LOGIN <username> <password>`：向 `/api/login` 发送 JSON；  
     - `QUIT`：退出客户端。  
   - 在 `displayResponse` 中区分文本响应与二进制响应，对非文本内容以“二进制内容 + 长度”形式提示，并保存到本地 `downloads/` 目录。  

2. **重定向与 301/302/304 处理**  
   - `handleRedirect(HttpResponse response, int maxRedirects)`：  
     - 若状态码为 301/302，读取 `Location` 头，自动发起新的 GET 请求，最多跟随 `maxRedirects` 次；  
     - 若状态码为 304，则输出提示“不修改（304）”，停止重定向处理。  

3. **GUI 客户端 `HttpClientGUI`**  
   - 使用 Swing 实现图形界面，并采用淡紫色系主题：  
     - 顶部：Host、Port 输入框和“测试连接”按钮；  
     - 中部：URI 输入、POST 文本请求体输入区、注册/登录输入（用户名 + 密码）、文件选择与上传按钮；  
     - 下方：响应显示区（可滚动、自动换行），以及状态栏。  
   - 内部复用 `HttpClient` 完成实际网络请求，界面层只负责收集用户输入并呈现结果。  
   - 通过 GUI 可以直观地测试：  
     - GET/POST 请求；  
     - 301/302 重定向（例如访问 `/old-page`、`/redirect-test`）；  
     - 登录/注册接口；  
     - 文件上传返回的 JSON 结果等。  

---

## 五、功能测试与结果

### 5.1 HTTP 基本功能测试

1. **200 OK**  
   - 访问 `/index.html` 或其它静态文件，浏览器/客户端显示页面内容，状态码为 200。  
   - 调用登录成功、注册成功、文件上传成功等接口也返回 200。  
2. **404 Not Found**  
   - 请求不存在的静态资源（如 `/no-such-page.html`），服务器返回 404，并优先展示自定义的 `404.html` 页面。  
3. **405 Method Not Allowed**  
   - 对静态资源使用非法方法（例如 `POST /index.html`），或对登录/注册接口使用非 POST 方法时，返回 405。  
4. **500 Internal Server Error**  
   - 当 Handler 内部抛出未预期异常时，`ConnectionHandler` 捕获后返回 500 错误页面，确保服务器不会直接崩溃。  

### 5.2 301 / 302 / 304 测试

1. **301 永久重定向**  
   - 请求 `/old-page`：  
     - 首次响应：状态码 301，`Location: /new-page.html`；  
     - 客户端 `handleRedirect` 自动跟随重定向，再次请求 `/new-page.html`，最终得到 200 响应。  

2. **302 临时重定向**  
   - 请求 `/redirect-test`：  
     - 首次响应：状态码 302，`Location: /new-page.html`；  
     - 客户端自动跟随重定向并获取新资源。  

3. **304 Not Modified**  
   - 访问某静态资源（如 `/index.html`）后，记录响应头中的 `Last-Modified`；  
   - 再次发送请求时，加上相同值的 `If-Modified-Since` 头，如果文件未发生变化，则服务器直接返回 304，不带响应体。  
   - 在浏览器的 Network 面板中也可以观察到 304 响应或缓存命中情况。  

### 5.3 注册 / 登录功能测试

1. **注册接口 `/api/register`**  

   - 使用 Postman 或自写客户端发送：  

     ```json
     POST /api/register
     Content-Type: application/json
     
     {"username": "alice", "password": "123456"}
     ```

   - 若用户名未存在且密码合法，返回：`{"status":"ok","message":"registered"}`，状态码 200；  

   - 重复注册或参数不合法时返回 400。  

2. **登录接口 `/api/login`**  

   - 发送：  

     ```json
     POST /api/login
     Content-Type: application/json
     
     {"username": "alice", "password": "123456"}
     ```

   - 验证通过时返回 200，Body `"Login successful!"`；  

   - 用户不存在或密码错误时返回 404。  

3. **数据存储方式**  

   - 所有用户信息存储在 `UserManager` 的内存 Map 中（运行期间有效），符合“无需持久化，可以存在内存中”的实验要求。  

---

## 六、实验总结与心得

1. **对 HTTP 协议的理解更深入**  
   - 亲手解析请求行、请求头与请求体，构造响应行和响应头，使得对 HTTP 报文结构有了直观认识；  
   - 实现 301/302/304、404、405、500 等状态码后，理解了不同状态码在浏览器和客户端中的具体含义与使用场景。  

2. **对 Socket 与长连接的实践**  
   - 使用 `ServerSocket` + `Socket` 构建了最基本的“应用层协议 + 传输层套接字”示例；  
   - 在单个 TCP 连接上循环处理多个 HTTP 请求，体会到 HTTP/1.1 长连接与短连接在实现上的差异。  

3. **模块化设计的好处**  
   - 通过 `HttpRequest` / `HttpResponse` 抽象报文，通过 `Router + Handler` 解耦路由与业务逻辑，使后续扩展新的路径和功能更加方便；  
   - 使用 `MimeType`、`UserManager` 等工具类集中管理 MIME 映射和用户数据，代码结构更加清晰。  

4. **GUI 与命令行的对比体验**  
   - 命令行客户端适合调试与快速测试；  
   - GUI 客户端通过更友好的界面区分 GET/POST、登录/注册、文件上传等功能，并用淡紫配色增强了可视化体验，适合课堂演示。  

整体而言，本实验完整走通了“从 Socket 到 HTTP 协议再到简单业务功能”的链路，既巩固了对网络编程基础的理解，也为后续学习更复杂的框架（如 Netty、Spring MVC 等）打下了良好基础。

## 七、测试过程截图（命令行）

### 7.0 启动客户端与服务端

1. 启动服务端，监听6175端口

![image-20251204232902632](https://hotori-typora.oss-cn-shanghai.aliyuncs.com/image-20251204232902632.png)

2. 启动客户端

![image-20251204233242636](https://hotori-typora.oss-cn-shanghai.aliyuncs.com/image-20251204233242636.png)

### 7.1 HTTP 基本功能测试

1. **200 OK**  

   - 访问 `/index.html` 或其它静态文件，浏览器/客户端显示页面内容，状态码为 200。  

   - 调用登录成功、注册成功、文件上传成功等接口也返回 200。  

   - 测试过程：

     1.1 测试连接

     ![image-20251204233327888](https://hotori-typora.oss-cn-shanghai.aliyuncs.com/image-20251204233327888.png)

     1.2 访问静态资源（支持文本类型和非文本类型）

     ![image-20251204233653707](https://hotori-typora.oss-cn-shanghai.aliyuncs.com/image-20251204233653707.png)

     ![image-20251204233814220](https://hotori-typora.oss-cn-shanghai.aliyuncs.com/image-20251204233814220.png)

     1.3 用户注册

     ![image-20251204234015865](https://hotori-typora.oss-cn-shanghai.aliyuncs.com/image-20251204234015865.png)

     1.4 用户登录

     ![image-20251204234044966](https://hotori-typora.oss-cn-shanghai.aliyuncs.com/image-20251204234044966.png)

     1.5 POST请求（支持文本类型和非文本类型）

     ![image-20251204234318956](https://hotori-typora.oss-cn-shanghai.aliyuncs.com/image-20251204234318956.png)

     ![image-20251204234224873](https://hotori-typora.oss-cn-shanghai.aliyuncs.com/image-20251204234224873.png)![image-20251205001915498](https://hotori-typora.oss-cn-shanghai.aliyuncs.com/image-20251205001915498.png)

2. **404 Not Found**  

   - 请求不存在的静态资源（如 `/no-such-page.html`），服务器返回 404，并优先展示自定义的 `404.html` 页面。  

     ![image-20251205002106571](https://hotori-typora.oss-cn-shanghai.aliyuncs.com/image-20251205002106571.png)

3. **405 Method Not Allowed**  

   - 对静态资源使用非法方法（例如 `POST /index.html`），或对登录/注册接口使用非 POST 方法时，返回 405。  

     ![image-20251205003021711](https://hotori-typora.oss-cn-shanghai.aliyuncs.com/image-20251205003021711.png)

4. **500 Internal Server Error**  

   - 当 Handler 内部抛出未预期异常时，`ConnectionHandler` 捕获后返回 500 错误页面，确保服务器不会直接崩溃。  ![image-20251205003751738](https://hotori-typora.oss-cn-shanghai.aliyuncs.com/image-20251205003751738.png)

### 7.2 301 / 302 / 304 测试

1. **301 永久重定向**  

   - 请求 `/old-page`：  

     - 首次响应：状态码 301，`Location: /new-page.html`；  

     - 客户端 `handleRedirect` 自动跟随重定向，再次请求 `/new-page.html`，最终得到 200 响应。  

       ![image-20251205004045043](https://hotori-typora.oss-cn-shanghai.aliyuncs.com/image-20251205004045043.png)

2. **302 临时重定向**  

   - 请求 `/redirect-test`：  
     - 首次响应：状态码 302，`Location: /new-page.html`；  
     - 客户端自动跟随重定向并获取新资源。  ![image-20251205004154776](https://hotori-typora.oss-cn-shanghai.aliyuncs.com/image-20251205004154776.png)

3. **304 Not Modified**  

   - 访问某静态资源（如 `/index.html`）后，记录响应头中的 `Last-Modified`；  
   - 再次发送请求时，加上相同值的 `If-Modified-Since` 头，如果文件未发生变化，则服务器直接返回 304，不带响应体。  
   - 在浏览器的 Network 面板中也可以观察到 304 响应或缓存命中情况。  ![image-20251205004623703](https://hotori-typora.oss-cn-shanghai.aliyuncs.com/image-20251205004623703.png)