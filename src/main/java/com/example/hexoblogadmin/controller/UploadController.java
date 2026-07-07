package com.example.hexoblogadmin.controller;

import com.example.hexoblogadmin.service.HexoService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class UploadController {

    private final HexoService hexoService;

    public UploadController(HexoService hexoService) {
        this.hexoService = hexoService;
    }

    @PostMapping
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return Map.of("url", hexoService.uploadImage(file));
    }
}
