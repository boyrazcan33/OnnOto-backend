package com.onnoto.onnoto_backend.repository;

import com.onnoto.onnoto_backend.model.AnonymousUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnonymousUserRepository extends JpaRepository<AnonymousUser, String> {
    // You can add custom query methods here if needed
}