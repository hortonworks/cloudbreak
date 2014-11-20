package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.Blueprint;

public interface BlueprintRepository extends CrudRepository<Blueprint, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    Blueprint findOne(@Param("id") Long id);

    @PostAuthorize("hasPermission(returnObject,'read')")
    Blueprint findByNameInAccount(@Param("name") String name, @Param("account") String account);

    Set<Blueprint> findForUser(@Param("user") String user);

    Set<Blueprint> findPublicsInAccount(@Param("account") String account);

    Set<Blueprint> findAllInAccount(@Param("account") String account);

}