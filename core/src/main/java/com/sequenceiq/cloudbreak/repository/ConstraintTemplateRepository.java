package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.ConstraintTemplate;

@EntityType(entityClass = ConstraintTemplate.class)
public interface ConstraintTemplateRepository extends CrudRepository<ConstraintTemplate, Long> {

    ConstraintTemplate findOne(@Param("id") Long id);

    Set<ConstraintTemplate> findForUser(@Param("user") String user);

    Set<ConstraintTemplate> findPublicInAccountForUser(@Param("user") String user, @Param("account") String account);

    Set<ConstraintTemplate> findAllInAccount(@Param("account") String account);

    ConstraintTemplate findOneByName(@Param("name") String name, @Param("account") String account);

    ConstraintTemplate findByNameInAccount(@Param("name") String name, @Param("account") String account, @Param("owner") String owner);

    ConstraintTemplate findByIdInAccount(@Param("id") Long id, @Param("account") String account);

    ConstraintTemplate findByNameInUser(@Param("name") String name, @Param("owner") String owner);

    Set<ConstraintTemplate> findAllDefaultInAccount(@Param("account") String account);

}
