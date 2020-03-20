package com.sequenceiq.datalake.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sequenceiq.authorization.repository.BaseCrudRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.projection.SdxClusterIdView;

@Repository
@AuthorizationResourceType(resource = AuthorizationResource.DATALAKE)
public interface SdxClusterRepository extends BaseCrudRepository<SdxCluster, Long> {

    @Override
    @CheckPermission(action = ResourceAction.READ)
    List<SdxCluster> findAll();

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s.id as id, s.stackCrn as stackCrn " +
            "FROM SdxCluster s " +
            "WHERE deleted is null")
    List<SdxClusterIdView> findAllAliveView();

    @CheckPermission(action = ResourceAction.READ)
    Optional<SdxCluster> findByAccountIdAndClusterNameAndDeletedIsNull(String accountId, String clusterName);

    @CheckPermission(action = ResourceAction.READ)
    Optional<SdxCluster> findByAccountIdAndCrnAndDeletedIsNull(String accountId, String crn);

    @CheckPermission(action = ResourceAction.READ)
    List<SdxCluster> findByAccountIdAndDeletedIsNull(String accountId);

    @CheckPermission(action = ResourceAction.READ)
    List<SdxCluster> findByAccountIdAndEnvCrnAndDeletedIsNull(String accountId, String envCrn);

    @CheckPermission(action = ResourceAction.READ)
    List<SdxCluster> findByAccountIdAndEnvNameAndDeletedIsNull(String accountId, String envName);

    @CheckPermission(action = ResourceAction.READ)
    List<SdxCluster> findByIdIn(Set<Long> resourceIds);

}
