package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.Blueprint;

@EntityType(entityClass = Blueprint.class)
public interface BlueprintRepository extends CrudRepository<Blueprint, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    Blueprint findOne(@Param("id") Long id);

    @PostAuthorize("hasPermission(returnObject,'read')")
    Blueprint findOneByName(@Param("name") String name, @Param("account") String account);

    Blueprint findByName(@Param("name") String name);

    Set<Blueprint> findForUser(@Param("user") String user);

    Set<Blueprint> findPublicInAccountForUser(@Param("user") String user, @Param("account") String account);

    Set<Blueprint> findAllInAccount(@Param("account") String account);

    Blueprint findByNameInAccount(@Param("name") String name, @Param("account") String account, @Param("owner") String owner);

    Blueprint findByIdInAccount(@Param("id") Long id, @Param("account") String account);

    Blueprint findByNameInUser(@Param("name") String name, @Param("owner") String owner);

    Set<Blueprint> findAllDefaultInAccount(@Param("account") String account);

}