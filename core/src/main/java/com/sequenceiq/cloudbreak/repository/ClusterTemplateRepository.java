package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.ClusterTemplate;

@EntityType(entityClass = ClusterTemplate.class)
public interface ClusterTemplateRepository extends CrudRepository<ClusterTemplate, Long> {

    ClusterTemplate findOne(@Param("id") Long id);

    ClusterTemplate findOneByName(@Param("name") String name, @Param("account") String account);

    Set<ClusterTemplate> findForUser(@Param("user") String user);

    Set<ClusterTemplate> findPublicInAccountForUser(@Param("user") String user, @Param("account") String account);

    Set<ClusterTemplate> findAllInAccount(@Param("account") String account);

    ClusterTemplate findByNameInAccount(@Param("name") String name, @Param("account") String account, @Param("owner") String owner);

    ClusterTemplate findByIdInAccount(@Param("id") Long id, @Param("account") String account);

    ClusterTemplate findByNameInUser(@Param("name") String name, @Param("owner") String owner);

    Set<ClusterTemplate> findAllDefaultInAccount(@Param("account") String account);

}