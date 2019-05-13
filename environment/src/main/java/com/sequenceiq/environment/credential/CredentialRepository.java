package com.sequenceiq.environment.credential;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@Transactional(TxType.REQUIRED)
interface CredentialRepository extends WorkspaceResourceRepository<Credential, Long> {

    @Query("SELECT c FROM Credential c WHERE c.workspace.id= :workspaceId AND c.archived IS FALSE AND cloudPlatform IN (:cloudPlatforms)")
    Set<Credential> findActiveForWorkspaceFilterByPlatforms(@Param("workspaceId") Long workspaceId, @Param("cloudPlatforms") Collection<String> cloudPlatforms);

    @Query("SELECT c FROM Credential c WHERE c.name= :name AND c.workspace.id= :workspaceId AND c.archived IS FALSE AND cloudPlatform IN (:cloudPlatforms)")
    Optional<Credential> findActiveByNameAndWorkspaceIdFilterByPlatforms(@Param("name") String name, @Param("workspaceId") Long workspaceId,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms);

    @Query("SELECT c FROM Credential c WHERE c.id= :id AND c.workspace.id= :workspaceId AND c.archived IS FALSE AND cloudPlatform IN (:cloudPlatforms)")
    Optional<Credential> findActiveByIdAndWorkspaceFilterByPlatforms(@Param("id") Long id, @Param("workspaceId") Long workspaceId,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms);

}