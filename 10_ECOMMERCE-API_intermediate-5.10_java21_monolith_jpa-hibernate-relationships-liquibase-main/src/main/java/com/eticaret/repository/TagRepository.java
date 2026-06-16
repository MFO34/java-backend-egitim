package com.eticaret.repository;

import com.eticaret.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findBySlug(String slug);

    boolean existsByName(String name);

    // İsim listesine göre tag'leri bul (toplu ürün etiketleme)
    List<Tag> findByNameIn(List<String> names);
}
