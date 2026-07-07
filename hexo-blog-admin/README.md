# Hexo 博客管理后台

一个基于 **Spring Boot** 的简易管理后台，部署到云服务器后，通过浏览器即可向 Hexo 博客 `source/_posts` 目录写入 Markdown 文章，并调用 `hexo generate` 重新生成静态站点。

## 项目结构

```
hexo-blog-admin
├── pom.xml
├── src/main/java/com/example/hexoblogadmin/
│   ├── HexoBlogAdminApplication.java
│   ├── controller/
│   │   ├── PostController.java        # 文章 API
│   │   ├── UploadController.java      # 图片上传 API
│   │   └── GlobalExceptionHandler.java
│   ├── model/
│   │   ├── Post.java
│   │   ├── PostCreateRequest.java
│   │   └── PostUpdateRequest.java
│   ├── service/
│   │   └── HexoService.java           # 文件读写 + hexo 命令执行
│   └── util/
│       ├── FrontMatterParser.java     # 解析 YAML front matter
│       └── SlugUtil.java              # 标题转文件名
└── src/main/resources/
    ├── application.properties
    └── static/                        # 前端页面
        ├── index.html
        ├── css/style.css
        └── js/app.js
```

## 功能

- 列出已有文章（左侧列表，按日期倒序）
- 新建 / 编辑 / 删除 Markdown 文章
- 自动写入 Hexo 标准 front matter：`title`、`date`、`categories`、`tags`
- 上传图片并自动插入 Markdown 图片语法
- 调用 `hexo generate` 重新生成静态站点
- 调用 `hexo clean` 清理缓存

## 后端接口

| 方法 | 接口 | 说明 |
|------|------|------|
| GET | `/api/posts` | 文章列表 |
| GET | `/api/posts/{slug}` | 单篇文章 |
| POST | `/api/posts` | 新建文章 |
| PUT | `/api/posts/{slug}` | 更新文章 |
| DELETE | `/api/posts/{slug}` | 删除文章 |
| POST | `/api/posts/generate` | 执行 `hexo generate` |
| POST | `/api/posts/clean` | 执行 `hexo clean` |
| POST | `/api/upload` | 上传图片，返回 `{url: "/images/xxx.png"}` |

## 本地开发

### 1. 修改配置

打开 `src/main/resources/application.properties`：

```properties
# 你的 Hexo 目录
blog.work-dir=/www/my-blog

# Hexo 可执行文件路径（本地安装）
blog.hexo-command=/www/my-blog/node_modules/.bin/hexo

# 如果服务器使用全局 hexo，可改为：
# blog.hexo-command=/usr/bin/hexo
```

### 2. 编译运行

```bash
# 在项目根目录执行
mvn clean package
java -jar target/hexo-blog-admin-1.0.0.jar
```

启动后访问：http://localhost:8081

## 部署到 Ubuntu 云服务器

假设你的服务器已安装 Java 17+、Node.js 和 Hexo，并且 Hexo 目录为 `/www/my-blog`。

### 1. 打包

```bash
mvn clean package
```

### 2. 上传 jar 到服务器

```bash
scp target/hexo-blog-admin-1.0.0.jar root@你的服务器IP:/opt/hexo-blog-admin/
```

### 3. 在服务器上运行

```bash
ssh root@你的服务器IP
cd /opt/hexo-blog-admin
nohup java -jar hexo-blog-admin-1.0.0.jar > app.log 2>&1 &
```

默认端口为 `8081`，请确保服务器防火墙放行该端口。

### 4. 使用 Nginx 反向代理（可选）

如果你希望使用域名访问，可以配置 Nginx：

```nginx
server {
    listen 80;
    server_name blog-admin.yourdomain.com;

    location / {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 5. 使用 systemd 常驻运行（推荐）

创建 `/etc/systemd/system/hexo-blog-admin.service`：

```ini
[Unit]
Description=Hexo Blog Admin
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/hexo-blog-admin
ExecStart=/usr/bin/java -jar /opt/hexo-blog-admin/hexo-blog-admin-1.0.0.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

启用并启动：

```bash
systemctl daemon-reload
systemctl enable hexo-blog-admin
systemctl start hexo-blog-admin
systemctl status hexo-blog-admin
```

## 注意事项

1. **当前版本没有登录验证**，部署到公网前务必通过 Nginx 添加 Basic Auth 或内网访问限制。
2. 程序运行用户需要对 `/www/my-blog/source/_posts` 和 `/www/my-blog/source/images` 有读写权限。
3. 如果点击“生成站点”没有生效，检查 `hexo-command` 路径是否正确，或在服务器上执行 `hexo generate` 看是否能正常生成。

## 技术栈

- Spring Boot 3.3 + Java 17
- 原生 HTML / CSS / JavaScript（无前端构建）
- Marked.js（Markdown 预览）
