hexo-blog-admin: 用于管理云服务器 Hexo 博客的 Spring Boot 后台。
- 技术栈：Spring Boot 3.3 + Java 17 + 原生 HTML/JS。
- 核心功能：文章 CRUD、图片上传、调用 hexo generate / clean。
- 关键配置：application.properties 中 `blog.work-dir` 与 `blog.hexo-command` 需指向服务器 Hexo 目录。
- 注意：当前无登录认证，公网部署前需加 Nginx Basic Auth 或访问限制。
