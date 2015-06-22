package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.SecurityGroup;

public interface SecurityGroupRepository extends CrudRepository<SecurityGroup, Long> {

    SecurityGroup findById(@Param("id") Long id);

    @PostAuthorize("hasPermission(returnObject,'read')")
    SecurityGroup findOneById(@Param("id") Long id);

    @PostAuthorize("hasPermission(returnObject,'read')")
    SecurityGroup findOneByName(@Param("name") String name, @Param("account") String account);

    SecurityGroup findByNameForUser(@Param("name") String name, @Param("owner") String userId);

    SecurityGroup findByNameInAccount(@Param("name") String name, @Param("account") String account);

    Set<SecurityGroup> findByName(@Param("name") String name);

    Set<SecurityGroup> findForUser(@Param("owner") String user);

    Set<SecurityGroup> findPublicInAccountForUser(@Param("owner") String user, @Param("account") String account);

    Set<SecurityGroup> findAllInAccount(@Param("account") String account);

    Set<SecurityGroup> findAllDefaultInAccount(@Param("account") String account);
}
