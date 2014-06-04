package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.Stack;

public interface StackRepository extends CrudRepository<Stack, Long> {

    @PostAuthorize("returnObject?.user?.id == principal?.id")
    Stack findOne(Long id);

    Stack findById(Long id);

    List<Stack> findAllStackForTemplate(@Param("id") Long id);
}