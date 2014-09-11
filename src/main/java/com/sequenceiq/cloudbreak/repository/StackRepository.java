package com.sequenceiq.cloudbreak.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.Stack;

public interface StackRepository extends CrudRepository<Stack, Long> {

    @PostAuthorize("returnObject?.owner == principal")
    Stack findOne(@Param("id") Long id);

    Stack findById(@Param("id") Long id);

    Stack findByName(@Param("name") String name);

    Set<Stack> findForUser(@Param("user") String user);

    Set<Stack> findPublicsInAccount(@Param("account") String account);

    Set<Stack> findAllInAccount(@Param("account") String account);

    Stack findOneWithLists(@Param("id") Long id);

    Stack findByStackResourceName(@Param("stackName") String stackName);

    List<Stack> findAllStackForTemplate(@Param("id") Long id);

    Stack findStackForCluster(@Param("id") Long id);

    Stack findStackWithListsForCluster(@Param("id") Long id);

    List<Stack> findRequestedStacksWithCredential(@Param("credentialId") Long credentialId);

    Stack findStackByHash(@Param("hash") String hash);
}