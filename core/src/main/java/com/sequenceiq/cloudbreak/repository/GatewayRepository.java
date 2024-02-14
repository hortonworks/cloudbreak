package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

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
            "g.signCert as signCertDeprecated, " +
            "g.signCertSecret as signCertSecret, " +
            "g.signPub as signPubDeprecated, " +
            "g.signPubSecret as signPubSecret, " +
            "g.tokenCert as tokenCertDeprecated, " +
            "g.tokenCertSecret as tokenCertSecret, " +
            "g.tokenKeySecret as tokenKeySecret, " +
            "g.tokenPubSecret as tokenPubSecret, " +
            "g.knoxMasterSecret as knoxMasterSecret, " +
            "gt as topologies, " +
            "g.gatewayPort as gatewayPort " +
            "FROM Gateway g " +
            "LEFT JOIN g.cluster c " +
            "LEFT JOIN g.topologies gt " +
            "WHERE c.id = :clusterId")
    Optional<GatewayViewDelegate> findByClusterId(@Param("clusterId") Long clusterId);
}
