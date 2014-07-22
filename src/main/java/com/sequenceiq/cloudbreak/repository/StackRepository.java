package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.Stack;

public interface StackRepository extends CrudRepository<Stack, Long> {

    @PostAuthorize("returnObject?.user?.id == principal?.id")
    Stack findOne(@Param("id") Long id);

    Stack findById(@Param("id") Long id);

    Stack findOneWithLists(@Param("id") Long id);

    Stack findByStackResourceName(@Param("stackName") String stackName);

    List<Stack> findAllStackForTemplate(@Param("id") Long id);

    Stack findStackForCluster(@Param("id") Long id);

    List<Stack> findRequestedStacksWithCredential(@Param("credentialId") Long credentialId);

    Stack findStackByHash(@Param("hash") String hash);
}