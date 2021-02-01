package com.sequenceiq.environment.credential.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.service.list.AuthorizationResource;
import com.sequenceiq.authorization.service.model.projection.ResourceCrnAndNameView;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.credential.domain.Credential;

@Transactional(TxType.REQUIRED)
public interface CredentialRepository extends JpaRepository<Credential, Long> {

    @Query("SELECT c FROM Credential c WHERE c.accountId = :accountId AND c.name = :name "
            + "AND c.archived IS FALSE AND cloudPlatform IN (:cloudPlatforms) AND c.type = :type")
    Optional<Credential> findByNameAndAccountId(
            @Param("name") String name,
            @Param("accountId") String accountId,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms,
            @Param("type") CredentialType type);

    @Query("SELECT c FROM Credential c WHERE c.accountId = :accountId AND c.resourceCrn = :crn "
            + "AND c.archived IS FALSE AND cloudPlatform IN (:cloudPlatforms) AND c.type = :type")
    Optional<Credential> findByCrnAndAccountId(
            @Param("crn") String crn,
            @Param("accountId") String accountId,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms,
            @Param("type") CredentialType type);

    @Query("SELECT c.name as name, c.resourceCrn as crn FROM Credential c WHERE c.accountId = :accountId AND c.resourceCrn IN (:resourceCrns)"
            + "AND c.archived IS FALSE AND cloudPlatform IN (:cloudPlatforms)")
    List<ResourceCrnAndNameView> findResourceNamesByCrnAndAccountId(
            @Param("resourceCrns") Collection<String> resourceCrns,
            @Param("accountId") String accountId,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms);

    @Query("SELECT c FROM Credential c WHERE c.accountId = :accountId AND c.archived IS FALSE AND cloudPlatform IN (:cloudPlatforms) AND c.type = :type")
    Set<Credential> findAllByAccountId(
            @Param("accountId") String accountId,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms,
            @Param("type") CredentialType type);

    @Query("SELECT c FROM Credential c JOIN Environment e ON e.credential.id = c.id WHERE e.resourceCrn = :envCrn AND c.accountId = :accountId "
            + "AND e.accountId = :accountId AND c.archived IS FALSE AND c.cloudPlatform IN (:cloudPlatforms) AND c.type = :type")
    Optional<Credential> findByEnvironmentCrnAndAccountId(
            @Param("envCrn") String envCrn,
            @Param("accountId") String accountId,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms,
            @Param("type") CredentialType type);

    @Query("SELECT c FROM Credential c JOIN Environment e ON e.credential.id = c.id WHERE e.name = :envName AND c.accountId = :accountId "
            + "AND e.accountId = :accountId AND c.archived IS FALSE AND c.cloudPlatform IN (:cloudPlatforms) AND c.type = :type")
    Optional<Credential> findByEnvironmentNameAndAccountId(
            @Param("envName") String envName,
            @Param("accountId") String accountId,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms,
            @Param("type") CredentialType type);

    @Query("SELECT c.resourceCrn FROM Credential c WHERE c.accountId = :accountId AND c.type = :type")
    List<String> findAllResourceCrnsByAccountId(
            @Param("accountId") String accountId,
            @Param("type") CredentialType type);

    @Query("SELECT new com.sequenceiq.authorization.service.list.AuthorizationResource(c.id, c.resourceCrn) FROM Credential c " +
            "WHERE c.accountId = :accountId AND c.archived IS FALSE AND cloudPlatform IN (:cloudPlatforms) AND c.type = :type")
    List<AuthorizationResource> findAsAuthorizationResourcesInAccountByType(
            @Param("accountId") String accountId,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms,
            @Param("type") CredentialType type);
}