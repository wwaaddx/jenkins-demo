package com.example.hexoblogadmin.util;

import com.example.hexoblogadmin.model.Post;

import java.util.ArrayList;
import java.util.List;

public class FrontMatterParser {

    /**
     * 解析 Hexo Markdown 文件的 front matter 与正文。
     *
     * @param text     文件全文
     * @param filename 文件名，用于生成 slug
     */
    public static Post parse(String text, String filename) {
        Post post = new Post();
        post.setSlug(filename.replaceFirst("\\.md$", ""));

        String[] lines = text.split("\n", -1);
        boolean inFrontMatter = false;
        boolean frontMatterClosed = false;
        String currentKey = null;
        List<String> currentList = null;

        for (String line : lines) {
            String trimmed = line.trim();

            if ("---".equals(trimmed)) {
                if (!inFrontMatter) {
                    inFrontMatter = true;
                    continue;
                } else {
                    frontMatterClosed = true;
                    break;
                }
            }

            if (!inFrontMatter) {
                continue;
            }

            // 列表项
            if (trimmed.startsWith("- ")) {
                if (currentKey != null && currentList != null) {
                    currentList.add(stripQuotes(trimmed.substring(2).trim()));
                }
                continue;
            }

            int colon = line.indexOf(':');
            if (colon > 0) {
                currentKey = line.substring(0, colon).trim();
                String value = stripQuotes(line.substring(colon + 1).trim());

                switch (currentKey) {
                    case "title" -> {
                        post.setTitle(value);
                        currentList = null;
                    }
                    case "date" -> {
                        post.setDate(value);
                        currentList = null;
                    }
                    case "categories", "category" -> {
                        List<String> categories = new ArrayList<>();
                        if (!value.isEmpty()) {
                            categories.add(value);
                        }
                        post.setCategories(categories);
                        currentList = categories;
                    }
                    case "tags", "tag" -> {
                        List<String> tags = new ArrayList<>();
                        if (!value.isEmpty()) {
                            tags.add(value);
                        }
                        post.setTags(tags);
                        currentList = tags;
                    }
                    default -> currentList = null;
                }
            }
        }

        // 提取正文
        int firstSeparator = text.indexOf("---");
        if (firstSeparator >= 0 && frontMatterClosed) {
            int secondSeparator = text.indexOf("---", firstSeparator + 3);
            if (secondSeparator >= 0) {
                int contentStart = text.indexOf("\n", secondSeparator + 3);
                if (contentStart >= 0) {
                    post.setContent(text.substring(contentStart + 1));
                } else {
                    post.setContent("");
                }
            }
        } else {
            post.setContent(text);
        }

        return post;
    }

    private static String stripQuotes(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("^['\"]|['\"]$", "");
    }
}
