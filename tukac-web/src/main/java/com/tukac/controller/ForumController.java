package com.tukac.controller;

import com.tukac.dto.ApiResponse;
import com.tukac.model.ForumComment;
import com.tukac.model.ForumPost;
import com.tukac.repository.ForumCommentRepository;
import com.tukac.repository.ForumPostRepository;
import com.tukac.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/forum")
public class ForumController {

    @Autowired private ForumPostRepository forumPostRepository;
    @Autowired private ForumCommentRepository forumCommentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private com.tukac.service.ActivityLogService activityLogService;

    // ── GET all forum posts ──────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPosts(@RequestParam(required = false) String search) {
        List<ForumPost> posts;
        if (search != null && !search.trim().isEmpty()) {
            posts = forumPostRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(search.trim(), search.trim());
        } else {
            posts = forumPostRepository.findAllByOrderByCreatedAtDesc();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (ForumPost post : posts) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", post.getId());
            map.put("title", post.getTitle());
            map.put("content", post.getContent());
            map.put("createdAt", post.getCreatedAt());
            map.put("authorId", post.getAuthorId());
            
            userRepository.findById(post.getAuthorId())
                    .ifPresent(u -> map.put("authorName", u.getName()));
            
            map.put("commentCount", forumCommentRepository.countByPostId(post.getId()));
            result.add(map);
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ── CREATE post ────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<ApiResponse<ForumPost>> createPost(@RequestBody Map<String, String> payload, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        String title = payload.get("title");
        String content = payload.get("content");

        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Title is required"));
        }
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Content is required"));
        }

        ForumPost post = new ForumPost();
        post.setTitle(title.trim());
        post.setContent(content.trim());
        post.setAuthorId(userId);

        ForumPost saved = forumPostRepository.save(post);
        userRepository.findById(userId).ifPresent(u -> saved.setAuthorName(u.getName()));
        activityLogService.log("CREATE_FORUM", "Started discussion: " + saved.getTitle());
        return ResponseEntity.ok(ApiResponse.ok("Discussion posted", saved));
    }

    // ── DELETE post ────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        Optional<ForumPost> opt = forumPostRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        
        ForumPost post = opt.get();
        boolean isOwner = post.getAuthorId().equals(userId);
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().matches("ROLE_(CHAIRPERSON|VICE-CHAIRPERSON)"));

        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(403).body(ApiResponse.error("Not authorised to delete this post"));
        }

        forumCommentRepository.deleteAllByPostId(id);
        forumPostRepository.deleteById(id);
        activityLogService.log("DELETE_FORUM", "Deleted discussion: " + post.getTitle());
        return ResponseEntity.ok(ApiResponse.ok("Post deleted", null));
    }

    // ── GET comments ───────────────────────────────────────────────
    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getComments(@PathVariable Long id) {
        List<ForumComment> comments = forumCommentRepository.findByPostIdOrderByCreatedAtAsc(id);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (ForumComment c : comments) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("postId", c.getPostId());
            map.put("authorId", c.getAuthorId());
            map.put("content", c.getContent());
            map.put("createdAt", c.getCreatedAt());
            userRepository.findById(c.getAuthorId()).ifPresent(u -> map.put("authorName", u.getName()));
            result.add(map);
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ── POST comment ───────────────────────────────────────────────
    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        String content = body.get("content");
        
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Comment cannot be empty"));
        }
        if (!forumPostRepository.existsById(id)) return ResponseEntity.notFound().build();

        ForumComment comment = new ForumComment();
        comment.setPostId(id);
        comment.setAuthorId(userId);
        comment.setContent(content.trim());
        ForumComment saved = forumCommentRepository.save(comment);

        Map<String, Object> result = new HashMap<>();
        result.put("id", saved.getId());
        result.put("postId", saved.getPostId());
        result.put("authorId", saved.getAuthorId());
        result.put("content", saved.getContent());
        result.put("createdAt", saved.getCreatedAt());
        userRepository.findById(userId).ifPresent(u -> result.put("authorName", u.getName()));

        return ResponseEntity.ok(ApiResponse.ok("Comment added", result));
    }

    // ── DELETE comment ─────────────────────────────────────────────
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        Optional<ForumComment> opt = forumCommentRepository.findById(commentId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        ForumComment comment = opt.get();
        boolean isOwner = comment.getAuthorId().equals(userId);
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().matches("ROLE_(CHAIRPERSON|VICE-CHAIRPERSON)"));

        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(403).body(ApiResponse.error("Not authorised to delete this comment"));
        }

        forumCommentRepository.deleteById(commentId);
        return ResponseEntity.ok(ApiResponse.ok("Comment deleted", null));
    }
}
