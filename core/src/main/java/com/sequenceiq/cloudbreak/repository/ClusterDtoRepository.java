package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.view.delegate.ClusterViewDelegate;

@org.springframework.stereotype.Repository
public interface ClusterDtoRepository extends Repository<Cluster, Long> {

    @Query("SELECT c.id as id, " +
            "c.creationFinished as creationFinished, " +
            "c.creationStarted as creationStarted, " +
            "c.upSince as upSince, " +
            "c.fqdn as fqdn, " +
            "c.clusterManagerIp as clusterManagerIp, " +
            "c.name as name, " +
            "c.description as description, " +
            "c.databaseServerCrn as databaseServerCrn, " +
            "c.rangerRazEnabled as rangerRazEnabled, " +
            "c.rangerRmsEnabled as rangerRmsEnabled," +
            "c.certExpirationState as certExpirationState, " +
            "c.environmentCrn as environmentCrn, " +
            "c.proxyConfigCrn as proxyConfigCrn, " +
            "c.uptime as uptime, " +
            "c.extendedBlueprintText as extendedBlueprintTextSecret, " +
            "c.attributes as attributesSecret, " +
            "c.customContainerDefinition as customContainerDefinition, " +
            "c.dpClusterManagerUser as dpClusterManagerUserSecret," +
            "c.dpClusterManagerPassword as dpClusterManagerPasswordSecret," +
            "c.password as passwordSecret, " +
            "c.userName as userNameSecret, " +
            "c.executorType as executorType, " +
            "c.variant as variant, " +
            "c.cloudbreakClusterManagerUser as cloudbreakClusterManagerUserSecretObject, " +
            "c.cloudbreakClusterManagerPassword as cloudbreakClusterManagerPasswordSecretObject, " +
            "c.cdpNodeStatusMonitorPassword as cdpNodeStatusMonitorPasswordSecret, " +
            "c.cloudbreakClusterManagerMonitoringPassword as cloudbreakClusterManagerMonitoringPasswordSecret, " +
            "c.cloudbreakClusterManagerMonitoringUser as cloudbreakClusterManagerMonitoringUserSecret, " +
            "c.databusCredential as databusCredentialSecret, " +
            "c.monitoringCredential as monitoringCredentialSecret, " +
            "c.embeddedDatabaseOnAttachedDisk as embeddedDatabaseOnAttachedDisk, " +
            "cc as customConfigurations, " +

            "c.keyStorePwd as keyStorePwdSecret, " +
            "c.trustStorePwd as trustStorePwdSecret, " +

            "f as fileSystem, " +
            "af as additionalFileSystem, " +
            "c.autoTlsEnabled as autoTlsEnabled, " +
            "c.dbSslRootCertBundle as dbSslRootCertBundle, " +
            "c.dbSslEnabled as dbSslEnabled, " +

            "c.encryptionProfileCrn as encryptionProfileCrn " +
            "FROM Cluster c " +
            "LEFT JOIN c.stack s " +
            "LEFT JOIN c.fileSystem f " +
            "LEFT JOIN c.additionalFileSystem af " +
            "LEFT JOIN c.customConfigurations cc " +
            "WHERE s.id = :stackId "
    )
    Optional<ClusterViewDelegate> findByStackId(@Param("stackId") Long stackId);
}
