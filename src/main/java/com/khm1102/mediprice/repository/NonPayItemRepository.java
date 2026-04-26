package com.khm1102.mediprice.repository;

import com.khm1102.mediprice.entity.NonPayItem;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NonPayItemRepository extends JpaRepository<NonPayItem, String> {
}
