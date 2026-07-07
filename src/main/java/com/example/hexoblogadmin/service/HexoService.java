package com.example.hexoblogadmin.service;

import com.example.hexoblogadmin.model.Post;
import com.example.hexoblogadmin.model.PostCreateRequest;
import com.example.hexoblogadmin.model.PostUpdateRequest;
import com.example.hexoblogadmin.util.FrontMatterParser;
import com.example.hexoblogadmin.util.SlugUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class HexoService {

    private static final Logger log = LoggerFactory.getLogger(HexoService.class);

    @Value("${blog.work-dir}")
    private String workDir;

    @Value("${blog.hexo-command}")
    private String hexoCommand;

    @Value("${blog.posts-dir}")
    private String postsDir;

    @Value("${blog.images-dir}")
    private String imagesDir;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<Post> listPosts() {
        List<Post> posts = new ArrayList<>();
        File dir = new File(postsDir);
        if (!dir.exists()) {
            return posts;
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".md"));
        if (files == null) {
            return posts;
        }
        for (File file : files) {
            posts.add(parseFile(file));
        }
        posts.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        return posts;
    }

    public Post getPost(String slug) {
        File file = getFile(slug);
        if (!file.exists()) {
            throw new IllegalArgumentException("文章不存在: " + slug);
        }
        return parseFile(file);
    }

    public Post createPost(PostCreateRequest request) {
        String slug = SlugUtil.toSlug(request.getTitle());
        File file = getFile(slug);
        if (file.exists()) {
            throw new IllegalArgumentException("文章已存在: " + slug);
        }

        Post post = new Post();
        post.setSlug(slug);
        post.setTitle(request.getTitle());
        post.setDate(LocalDateTime.now().format(DATE_FORMAT));
        post.setCategories(emptyIfNull(request.getCategories()));
        post.setTags(emptyIfNull(request.getTags()));
        post.setContent(request.getContent());

        saveFile(post);
        return post;
    }

    public Post updatePost(String slug, PostUpdateRequest request) {
        File file = getFile(slug);
        if (!file.exists()) {
            throw new IllegalArgumentException("文章不存在: " + slug);
        }

        Post post = parseFile(file);
        post.setTitle(request.getTitle());
        post.setCategories(emptyIfNull(request.getCategories()));
        post.setTags(emptyIfNull(request.getTags()));
        post.setContent(request.getContent());

        saveFile(post);
        return post;
    }

    public void deletePost(String slug) {
        File file = getFile(slug);
        if (!file.exists()) {
            throw new IllegalArgumentException("文章不存在: " + slug);
        }
        if (!file.delete()) {
            throw new RuntimeException("删除文件失败: " + file.getAbsolutePath());
        }
    }

    public Map<String, String> runHexo(String command) throws Exception {
        log.info("执行 hexo {} (workDir={}, command={})", command, workDir, hexoCommand);
        ProcessBuilder pb = new ProcessBuilder(hexoCommand, command);
        pb.directory(new File(workDir));
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            log.error("hexo {} 执行失败, exitCode={}\n{}", command, exitCode, output);
        } else {
            log.info("hexo {} 执行成功", command);
        }

        Map<String, String> result = new LinkedHashMap<>();
        result.put("command", command);
        result.put("exitCode", String.valueOf(exitCode));
        result.put("output", output);
        return result;
    }

    public String uploadImage(MultipartFile file) throws IOException {
        File dir = new File(imagesDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("无法创建图片目录: " + imagesDir);
        }

        String original = file.getOriginalFilename();
        String ext = ".png";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String filename = UUID.randomUUID() + ext;
        File dest = new File(dir, filename);
        file.transferTo(dest);

        return "/images/" + filename;
    }

    private File getFile(String slug) {
        return new File(postsDir, slug + ".md");
    }

    private Post parseFile(File file) {
        try {
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return FrontMatterParser.parse(text, file.getName());
        } catch (IOException e) {
            throw new RuntimeException("读取文章失败: " + file.getAbsolutePath(), e);
        }
    }

    private void saveFile(Post post) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("title: ").append(yamlQuote(post.getTitle())).append("\n");
        sb.append("date: ").append(post.getDate()).append("\n");

        if (!post.getCategories().isEmpty()) {
            sb.append("categories:\n");
            for (String c : post.getCategories()) {
                sb.append("  - ").append(yamlQuote(c)).append("\n");
            }
        }

        if (!post.getTags().isEmpty()) {
            sb.append("tags:\n");
            for (String t : post.getTags()) {
                sb.append("  - ").append(yamlQuote(t)).append("\n");
            }
        }

        sb.append("---\n\n");
        sb.append(post.getContent());

        try {
            File file = getFile(post.getSlug());
            Files.writeString(file.toPath(), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("保存文章失败: " + post.getSlug(), e);
        }
    }

    private String yamlQuote(String s) {
        if (s == null) {
            return "\"\"";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private List<String> emptyIfNull(List<String> list) {
        return list == null ? new ArrayList<>() : list;
    }
}
