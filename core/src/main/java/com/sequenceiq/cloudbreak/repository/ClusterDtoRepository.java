package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.stack.ClusterDto;

@org.springframework.stereotype.Repository
public interface ClusterDtoRepository extends Repository<Cluster, Long> {

    @Query("SELECT c.id as id, " +
            "c.creationFinished as creationFinished, " +
            "c.upSince as upSince, " +
            "c.fqdn as fqdn, " +
            "c.clusterManagerIp as clusterManagerIp, " +
            "c.name as name, " +
            "c.description as description, " +
            "c.databaseServerCrn as databaseServerCrn, " +
            "c.rangerRazEnabled as rangerRazEnabled, " +
            "c.certExpirationState as certExpirationState, " +
            "c.environmentCrn as environmentCrn, " +
            "c.proxyConfigCrn as proxyConfigCrn, " +
            "c.uptime as uptime, " +
            "c.extendedBlueprintText as extendedBlueprintText, " +
            "c.attributes as attributes, " +
            "c.customContainerDefinition as customContainerDefinition, " +
            "c.dpAmbariUser as dpAmbariUser, " +
            "c.dpAmbariPassword as dpAmbariPassword, " +
            "f as fileSystem, " +
            "af as additionalFileSystem, " +
            "c.autoTlsEnabled as autoTlsEnabled " +
            "FROM Cluster c " +
            "LEFT JOIN c.stack s " +
            "LEFT JOIN c.fileSystem f " +
            "LEFT JOIN c.additionalFileSystem af " +
            "WHERE s.id = :stackId "
    )
    Optional<ClusterDto> findByStackId(@Param("stackId") Long stackId);
}
