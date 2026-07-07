package com.example.hexoblogadmin.util;

import java.text.Normalizer;

public class SlugUtil {

    /**
     * 将标题转换为可用作文件名/URL 的 slug。
     * 保留中文、英文、数字，其余字符替换为短横线。
     */
    public static String toSlug(String title) {
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD);
        String slug = normalized
                .replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.isEmpty()) {
            slug = "post-" + System.currentTimeMillis();
        }
        return slug;
    }
}
