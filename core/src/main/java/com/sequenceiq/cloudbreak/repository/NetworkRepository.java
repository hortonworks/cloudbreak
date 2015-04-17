package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.Network;

public interface NetworkRepository extends CrudRepository<Network, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    Network findOneById(@Param("id") Long id);

    @PostAuthorize("hasPermission(returnObject,'read')")
    Network findOneByName(@Param("name") String name, @Param("account") String account);

    Network findByNameForUser(@Param("name") String name, @Param("owner") String userId);

    Network findByNameInAccount(@Param("name") String name, @Param("account") String account);

    Set<Network> findByName(@Param("name") String name);

    Set<Network> findForUser(@Param("owner") String user);

    Set<Network> findPublicInAccountForUser(@Param("owner") String user, @Param("account") String account);

    Set<Network> findAllInAccount(@Param("account") String account);

    Set<Network> findAllDefaultInAccount(@Param("account") String account);
}
