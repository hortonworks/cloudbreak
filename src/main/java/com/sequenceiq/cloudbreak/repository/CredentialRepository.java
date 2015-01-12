package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.Credential;

public interface CredentialRepository extends CrudRepository<Credential, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    Credential findOne(@Param("id") Long id);

    @PostAuthorize("hasPermission(returnObject,'read')")
    Credential findOneByName(@Param("name") String name, @Param("account") String account);

    Set<Credential> findForUser(@Param("user") String user);

    Set<Credential> findPublicInAccountForUser(@Param("user") String user, @Param("account") String account);

    Set<Credential> findAllInAccount(@Param("account") String account);

    Credential findByNameInAccount(@Param("name") String name, @Param("account") String account, @Param("owner") String owner);

    Credential findByIdInAccount(@Param("id") Long id, @Param("account") String account, @Param("owner") String owner);

    Credential findByNameInUser(@Param("name") String name, @Param("owner") String owner);

}