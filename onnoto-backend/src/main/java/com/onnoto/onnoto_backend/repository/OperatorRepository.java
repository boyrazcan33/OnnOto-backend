package com.onnoto.onnoto_backend.repository;

import com.onnoto.onnoto_backend.model.Operator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperatorRepository extends JpaRepository<Operator, String> {
    // You can add custom query methods here if needed
}