package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.view.delegate.GatewayViewDelegate;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = Gateway.class)
@Transactional(TxType.REQUIRED)
public interface GatewayRepository extends CrudRepository<Gateway, Long> {

    @Query("SELECT g.id as id, " +
            "g.gatewayType as gatewayType, " +
            "g.path as path, " +
            "g.ssoType as ssoType, " +
            "g.ssoProvider as ssoProvider, " +
            "g.signKey as signKeySecret, " +
            "g.signCert as signCert, " +
            "g.signPub as signPub, " +
            "g.tokenCert as tokenCert, " +
            "g.knoxMasterSecret as knoxMasterSecret, " +
            "gt as topologies, " +
            "g.gatewayPort as gatewayPort " +
            "FROM Gateway g " +
            "LEFT JOIN g.cluster c " +
            "LEFT JOIN g.topologies gt " +
            "WHERE c.id = :clusterId")
    Optional<GatewayViewDelegate> findByClusterId(@Param("clusterId") Long clusterId);

    @Query("SELECT count (g) > 0 " +
            "FROM Gateway g " +
            "LEFT JOIN g.cluster c " +
            "WHERE c.id = :clusterId")
    Boolean existsByClusterId(@Param("clusterId") Long clusterId);

}
