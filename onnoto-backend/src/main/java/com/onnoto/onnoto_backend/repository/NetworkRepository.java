package com.onnoto.onnoto_backend.repository;

import com.onnoto.onnoto_backend.model.Network;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NetworkRepository extends JpaRepository<Network, String> {
    // You can add custom query methods here if needed
}