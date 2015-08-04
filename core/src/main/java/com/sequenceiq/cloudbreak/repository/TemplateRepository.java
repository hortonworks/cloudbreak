package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Template;

public interface TemplateRepository extends CrudRepository<Template, Long> {

    Template findOne(@Param("id") Long id);

    Set<Template> findForUser(@Param("user") String user);

    Set<Template> findPublicInAccountForUser(@Param("user") String user, @Param("account") String account);

    Set<Template> findAllInAccount(@Param("account") String account);

    Template findOneByName(@Param("name") String name, @Param("account") String account);

    Template findByNameInAccount(@Param("name") String name, @Param("account") String account, @Param("owner") String owner);

    Template findByIdInAccount(@Param("id") Long id, @Param("account") String account);

    Template findByNameInUser(@Param("name") String name, @Param("owner") String owner);

    Set<Template> findAllDefaultInAccount(@Param("account") String account);

}