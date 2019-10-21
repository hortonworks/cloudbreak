package com.sequenceiq.environment.credential.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.repository.BaseJpaRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.environment.credential.domain.CredentialView;

@Transactional(TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface CredentialViewRepository extends BaseJpaRepository<CredentialView, Long> {

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT c FROM CredentialView c WHERE c.accountId = :accountId AND c.name = :name "
            + "AND c.archived IS FALSE AND cloudPlatform IN (:cloudPlatforms)")
    Optional<CredentialView> findByNameAndAccountId(
            @Param("name") String name,
            @Param("accountId") String accountId,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT c FROM CredentialView c WHERE c.accountId = :accountId AND c.resourceCrn = :crn "
            + "AND c.archived IS FALSE AND cloudPlatform IN (:cloudPlatforms)")
    Optional<CredentialView> findByCrnAndAccountId(
            @Param("crn") String crn,
            @Param("accountId") String accountId,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT c FROM CredentialView c WHERE c.accountId = :accountId AND c.archived IS FALSE AND cloudPlatform IN (:cloudPlatforms)")
    Set<CredentialView> findAllByAccountId(
            @Param("accountId") String accountId,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT c FROM CredentialView c JOIN Environment e ON e.credential.id = c.id WHERE e.resourceCrn = :envCrn AND c.accountId = :accountId "
            + "AND e.accountId = :accountId AND c.archived IS FALSE AND c.cloudPlatform IN (:cloudPlatforms)")
    Optional<CredentialView> findByEnvironmentCrnAndAccountId(
            @Param("envCrn") String envCrn,
            @Param("accountId") String accountId,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms);

    @Query("SELECT c FROM CredentialView c JOIN Environment e ON e.credential.id = c.id WHERE e.name = :envName AND c.accountId = :accountId "
            + "AND e.accountId = :accountId AND c.archived IS FALSE AND c.cloudPlatform IN (:cloudPlatforms)")
    Optional<CredentialView> findByEnvironmentNameAndAccountId(
            @Param("envName") String envName,
            @Param("accountId") String accountId,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms);
}