package com.tukac.controller;

import com.tukac.dto.ApiResponse;
import com.tukac.model.BlogComment;
import com.tukac.model.BlogLike;
import com.tukac.model.BlogPost;
import com.tukac.repository.BlogCommentRepository;
import com.tukac.repository.BlogLikeRepository;
import com.tukac.repository.BlogPostRepository;
import com.tukac.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/blog")
public class BlogController {

    /**
     * Repositories are automatically injected by Spring's Dependency Injection.
     * They handle the data persistence for blog posts, likes, comments, and user info.
     */
    @Autowired private BlogPostRepository blogPostRepository;
    @Autowired private BlogCommentRepository commentRepository;
    @Autowired private BlogLikeRepository likeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private com.tukac.service.ActivityLogService activityLogService;

    /**
     * BROWSE/SEARCH: Retrieves all blog posts.
     * Includes logic to check if the current user has liked each post.
     * Maps the database entities to a list of data-transfer maps for the frontend.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPosts(
            @RequestParam(required = false) String search,
            Authentication auth) {
        Long userId = auth != null ? (Long) auth.getCredentials() : null;
        List<BlogPost> posts;
        if (search != null && !search.isEmpty()) {
            // Search logic using repository finders
            posts = blogPostRepository.findByTitleContainingIgnoreCaseOrBodyContainingIgnoreCase(search, search);
        } else {
            // Default: List all posts from newest to oldest
            posts = blogPostRepository.findAllByOrderByPublishedAtDesc();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (BlogPost post : posts) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", post.getId());
            map.put("title", post.getTitle());
            map.put("content", post.getContent());
            map.put("body", post.getBody());
            map.put("category", post.getCategory());
            map.put("publishedAt", post.getPublishedAt());
            map.put("mediaUrl", post.getMediaUrl());
            map.put("mediaType", post.getMediaType());
            map.put("authorId", post.getAuthorId());
            
            // Enrich data with author name
            if (post.getAuthorId() != null) {
                userRepository.findById(post.getAuthorId())
                        .ifPresent(u -> map.put("authorName", u.getName()));
            }
            
            // Add interaction statistics
            map.put("likeCount", likeRepository.countByPostId(post.getId()));
            map.put("commentCount", commentRepository.countByPostId(post.getId()));
            map.put("liked", userId != null && likeRepository.existsByPostIdAndUserId(post.getId(), userId));
            result.add(map);
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * ADD: Creates a new blog post.
     * Restricted to specific administrative roles via @PreAuthorize.
     * Automatically captures the author's ID from the authentication token.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CHAIRPERSON','VICE-CHAIRPERSON','SECRETARY')")
    public ResponseEntity<ApiResponse<BlogPost>> createPost(@RequestBody Map<String, Object> payload, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        String title = (String) payload.get("title");
        String content = (String) payload.get("content");
        String category = (String) payload.get("category");
        String mediaUrl = (String) payload.get("mediaUrl");
        String mediaType = (String) payload.get("mediaType");

        // Basic validation
        if (title == null || title.isBlank()) return ResponseEntity.badRequest().body(ApiResponse.error("Title is required"));
        if (content == null || content.isBlank()) return ResponseEntity.badRequest().body(ApiResponse.error("Content is required"));

        BlogPost post = new BlogPost();
        post.setTitle(title);
        post.setContent(content);
        post.setCategory(category);
        post.setAuthorId(userId);
        post.setMediaUrl(mediaUrl);
        post.setMediaType(mediaType);

        BlogPost saved = blogPostRepository.save(post);
        userRepository.findById(userId).ifPresent(u -> saved.setAuthorName(u.getName()));
        
        // Log activity for audit trail
        activityLogService.log("CREATE_BLOG", "Published blog post: " + saved.getTitle());
        return ResponseEntity.ok(ApiResponse.ok("Post published", saved));
    }

    /**
     * UPDATE/EDIT: Modifies an existing blog post.
     * Checks if the post exists and then updates allowed fields.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIRPERSON','VICE-CHAIRPERSON','SECRETARY')")
    public ResponseEntity<ApiResponse<BlogPost>> updatePost(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Optional<BlogPost> opt = blogPostRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        BlogPost post = opt.get();
        String title = (String) payload.get("title");
        String content = (String) payload.get("content");
        String category = (String) payload.get("category");

        if (title != null && !title.isBlank()) post.setTitle(title);
        if (content != null && !content.isBlank()) post.setContent(content);
        if (category != null) post.setCategory(category);
        if (payload.containsKey("mediaUrl")) post.setMediaUrl((String) payload.get("mediaUrl"));
        if (payload.containsKey("mediaType")) post.setMediaType((String) payload.get("mediaType"));

        BlogPost saved = blogPostRepository.save(post);
        if (saved.getAuthorId() != null) {
            userRepository.findById(saved.getAuthorId()).ifPresent(u -> saved.setAuthorName(u.getName()));
        }
        activityLogService.log("UPDATE_BLOG", "Updated blog post: " + saved.getTitle());
        return ResponseEntity.ok(ApiResponse.ok("Post updated", saved));
    }

    /**
     * DELETE: Removes a blog post and its associated likes/comments.
     * Cascading delete is handled manually here for data integrity.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIRPERSON','VICE-CHAIRPERSON','SECRETARY')")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
        likeRepository.deleteAllByPostId(id);
        commentRepository.deleteAllByPostId(id);
        BlogPost post = blogPostRepository.findById(id).get();
        blogPostRepository.deleteById(id);
        activityLogService.log("DELETE_BLOG", "Deleted blog post: " + post.getTitle());
        return ResponseEntity.ok(ApiResponse.ok("Post deleted", null));
    }

    /**
     * MEDIA HANDLING: Converts uploaded files into Base64 data URLs.
     * This allows the club to store images/videos directly in the SQLite database without 
     * complex filesystem management.
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('CHAIRPERSON','VICE-CHAIRPERSON','SECRETARY')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadMedia(@RequestParam("file") MultipartFile file) {
        try {
            String contentType = file.getContentType();
            String mediaType;
            if (contentType != null && contentType.startsWith("video/")) mediaType = "video";
            else if (contentType != null && contentType.startsWith("image/")) mediaType = "image";
            else return ResponseEntity.badRequest().body(ApiResponse.error("Only image or video files are allowed"));

            // Conversion to Base64
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            String dataUrl = "data:" + contentType + ";base64," + base64;
            return ResponseEntity.ok(ApiResponse.ok("File uploaded", Map.of("mediaUrl", dataUrl, "mediaType", mediaType)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Upload failed: " + e.getMessage()));
        }
    }

    /**
     * SOCIAL INTERACTION: Likes or Unlikes a post.
     * Checks if a like already exists for the user; if so, it removes it (Unlike).
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleLike(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        if (!blogPostRepository.existsById(id)) return ResponseEntity.notFound().build();

        boolean liked;
        Optional<BlogLike> existing = likeRepository.findByPostIdAndUserId(id, userId);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            liked = false;
        } else {
            likeRepository.save(new BlogLike(id, userId));
            liked = true;
        }
        long count = likeRepository.countByPostId(id);
        return ResponseEntity.ok(ApiResponse.ok(liked ? "Liked" : "Unliked", Map.of("liked", liked, "likeCount", count)));
    }

    /**
     * COMMENTS: Retrieves all comments for a specific post.
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getComments(@PathVariable Long id) {
        List<BlogComment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(id);
        List<Map<String, Object>> result = new ArrayList<>();
        for (BlogComment c : comments) {
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

    /**
     * COMMENTS: Adds a new comment to a post.
     */
    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        String content = body.get("content");
        if (content == null || content.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.error("Comment cannot be empty"));
        if (!blogPostRepository.existsById(id)) return ResponseEntity.notFound().build();

        BlogComment comment = new BlogComment();
        comment.setPostId(id);
        comment.setAuthorId(userId);
        comment.setContent(content.trim());
        BlogComment saved = commentRepository.save(comment);

        Map<String, Object> result = new HashMap<>();
        result.put("id", saved.getId());
        result.put("postId", saved.getPostId());
        result.put("authorId", saved.getAuthorId());
        result.put("content", saved.getContent());
        result.put("createdAt", saved.getCreatedAt());
        userRepository.findById(userId).ifPresent(u -> result.put("authorName", u.getName()));

        return ResponseEntity.ok(ApiResponse.ok("Comment added", result));
    }

    /**
     * COMMENTS: Deletes a comment.
     * Authorization Check: Only the comment author or a club executive can delete comments.
     */
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        Optional<BlogComment> opt = commentRepository.findById(commentId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        BlogComment comment = opt.get();
        boolean isOwner = comment.getAuthorId().equals(userId);
        
        // Role check logic for complex authorisation
        boolean isManager = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().matches("ROLE_(CHAIRPERSON|VICE-CHAIRPERSON|SECRETARY)"));

        if (!isOwner && !isManager)
            return ResponseEntity.status(403).body(ApiResponse.error("Not authorised to delete this comment"));

        commentRepository.deleteById(commentId);
        return ResponseEntity.ok(ApiResponse.ok("Comment deleted", null));
    }
}
