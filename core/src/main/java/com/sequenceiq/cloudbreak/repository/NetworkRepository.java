package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Topology;

@EntityType(entityClass = Network.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface NetworkRepository extends CrudRepository<Network, Long> {

    @PostAuthorize("hasPermission(returnObject,'read')")
    @Query("SELECT r FROM Network r WHERE r.id= :id")
    Network findOneById(@Param("id") Long id);

    @PostAuthorize("hasPermission(returnObject,'read')")
    @Query("SELECT r FROM Network r WHERE r.name= :name AND r.status <> 'DEFAULT_DELETED'")
    Network findOneByName(@Param("name") String name);

    @Query("SELECT r FROM Network r WHERE r.name= :name AND r.owner= :owner AND r.status <> 'DEFAULT_DELETED'")
    Network findByNameForUser(@Param("name") String name, @Param("owner") String userId);

    @Query("SELECT r FROM Network r WHERE r.name= :name AND r.account= :account AND r.status <> 'DEFAULT_DELETED'")
    Network findByNameInAccount(@Param("name") String name, @Param("account") String account);

    @Query("SELECT r FROM Network r WHERE r.owner= :owner AND r.status <> 'DEFAULT_DELETED'")
    Set<Network> findForUser(@Param("owner") String user);

    @Query("SELECT r FROM Network r WHERE ((r.account= :account AND r.publicInAccount= true) OR r.owner= :owner) AND r.status <> 'DEFAULT_DELETED'")
    Set<Network> findPublicInAccountForUser(@Param("owner") String user, @Param("account") String account);

    @Query("SELECT r FROM Network r WHERE r.account= :account AND r.status <> 'DEFAULT_DELETED'")
    Set<Network> findAllInAccount(@Param("account") String account);

    @Query("SELECT r FROM Network r WHERE r.account= :account AND (r.status = 'DEFAULT_DELETED' OR r.status = 'DEFAULT') ")
    Set<Network> findAllDefaultInAccount(@Param("account") String account);

    Long countByTopology(Topology topology);
}
