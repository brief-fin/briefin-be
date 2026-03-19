package com.briefin.domain.news.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/news")
public class NewsController {

    @GetMapping
    public String getNews() {
        return "news list";
    }

    @GetMapping("/{id}")
    public String getNewsById(@PathVariable Long id) {
        return "news " + id;
    }

    @PostMapping
    public String createNews() {
        return "news created";
    }

    @PutMapping("/{id}")
    public String updateNews(@PathVariable Long id) {
        return "news " + id + " updated";
    }

    @DeleteMapping("/{id}")
    public String deleteNews(@PathVariable Long id) {
        return "news " + id + " deleted";
    }
    
}
