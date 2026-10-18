package com.devbridge.logicore.repository;

import com.devbridge.logicore.model.FreightAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FreightAssignmentRepository
        extends JpaRepository<FreightAssignment, Long> {
}