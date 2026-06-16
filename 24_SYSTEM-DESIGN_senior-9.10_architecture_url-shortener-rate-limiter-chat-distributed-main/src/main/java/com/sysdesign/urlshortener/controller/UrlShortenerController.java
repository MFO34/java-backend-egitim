package com.sysdesign.urlshortener.controller;

import com.sysdesign.urlshortener.service.UrlShortenerService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/url")
public class UrlShortenerController {

    private final UrlShortenerService service;

    public UrlShortenerController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping("/shorten")
    public ResponseEntity<Map<String, String>> shorten(
            @RequestParam String url,
            @RequestParam(defaultValue = "365") int ttlDays) {
        String code = service.shorten(url, Duration.ofDays(ttlDays));
        return ResponseEntity.ok(Map.of(
                "code", code,
                "shortUrl", "http://short.ly/" + code,
                "originalUrl", url
        ));
    }

    // 301 Redirect (permanent) vs 302 (temporary)
    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        return service.resolve(code)
                .map(url -> ResponseEntity.<Void>status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, url)
                        .build())
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{code}/stats")
    public ResponseEntity<Map<String, Object>> stats(@PathVariable String code) {
        return service.resolve(code)
                .map(url -> ResponseEntity.ok(Map.<String, Object>of(
                        "code", code,
                        "originalUrl", url,
                        "clicks", service.getClickCount(code)
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
