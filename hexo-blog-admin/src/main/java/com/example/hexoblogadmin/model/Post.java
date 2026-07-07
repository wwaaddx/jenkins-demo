package com.example.hexoblogadmin.model;

import lombok.Data;

import java.util.List;

@Data
public class Post {

    /** URL 与文件名标识（不带 .md） */
    private String slug;

    private String title;

    /** yyyy-MM-dd HH:mm:ss */
    private String date;

    private List<String> categories;

    private List<String> tags;

    /** Markdown 正文，不含 front matter */
    private String content;
}
