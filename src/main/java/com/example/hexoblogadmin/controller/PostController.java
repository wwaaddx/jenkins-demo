package com.example.hexoblogadmin.controller;

import com.example.hexoblogadmin.model.Post;
import com.example.hexoblogadmin.model.PostCreateRequest;
import com.example.hexoblogadmin.model.PostUpdateRequest;
import com.example.hexoblogadmin.service.HexoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*")
public class PostController {

    private final HexoService hexoService;

    public PostController(HexoService hexoService) {
        this.hexoService = hexoService;
    }

    @GetMapping
    public List<Post> list() {
        return hexoService.listPosts();
    }

    @GetMapping("/{slug}")
    public Post get(@PathVariable String slug) {
        return hexoService.getPost(slug);
    }

    @PostMapping
    public Post create(@RequestBody PostCreateRequest request) {
        return hexoService.createPost(request);
    }

    @PutMapping("/{slug}")
    public Post update(@PathVariable String slug, @RequestBody PostUpdateRequest request) {
        return hexoService.updatePost(slug, request);
    }

    @DeleteMapping("/{slug}")
    public void delete(@PathVariable String slug) {
        hexoService.deletePost(slug);
    }

    @PostMapping("/generate")
    public Map<String, String> generate() throws Exception {
        return hexoService.runHexo("generate");
    }

    @PostMapping("/clean")
    public Map<String, String> clean() throws Exception {
        return hexoService.runHexo("clean");
    }
}
