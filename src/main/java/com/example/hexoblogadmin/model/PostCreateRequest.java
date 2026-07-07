package com.example.hexoblogadmin.model;

import lombok.Data;

import java.util.List;

@Data
public class PostCreateRequest {

    private String title;
    private String content;
    private List<String> categories;
    private List<String> tags;
}
