package com.tukac.repository;

import com.tukac.model.ForumPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {
    List<ForumPost> findAllByOrderByCreatedAtDesc();
    List<ForumPost> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(String title, String content);
}
