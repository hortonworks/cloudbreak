package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.Template;

public interface TemplateRepository extends CrudRepository<Template, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    Template findOne(@Param("id") Long id);

    Template findByName(@Param("name") String name);

    Set<Template> findForUser(@Param("user") String user);

    Set<Template> findPublicsInAccount(@Param("account") String account);

    Set<Template> findAllInAccount(@Param("account") String account);
}