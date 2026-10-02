package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.NewsArticle;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID> {

    List<NewsArticle> findAllByOrderByPublishedDateDescDisplayOrderAsc();

    List<NewsArticle> findByActiveTrueOrderByPublishedDateDescDisplayOrderAsc();

    List<NewsArticle> findAllByOrderByDisplayOrderAsc();
}
