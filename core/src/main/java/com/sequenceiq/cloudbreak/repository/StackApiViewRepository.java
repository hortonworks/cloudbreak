package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = StackApiView.class)
@Transactional(TxType.REQUIRED)
@WorkspaceResourceType(resource = WorkspaceResource.STACK)
public interface StackApiViewRepository extends WorkspaceResourceRepository<StackApiView, Long> {

    @CheckPermissionsByReturnValue
    @Query("SELECT s FROM StackApiView s LEFT JOIN FETCH s.cluster c LEFT JOIN FETCH c.clusterDefinition "
            + "LEFT JOIN FETCH c.hostGroups hg LEFT JOIN FETCH hg.hostMetadata "
            + "LEFT JOIN FETCH s.credential LEFT JOIN FETCH s.stackStatus LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "LEFT JOIN FETCH s.userView LEFT JOIN FETCH s.environment LEFT JOIN FETCH c.kerberosConfig "
            + "WHERE s.id= :id")
    Optional<StackApiView> findById(@Param("id") Long id);

    @CheckPermissionsByWorkspaceId
    @Query("SELECT s FROM StackApiView s LEFT JOIN FETCH s.cluster c LEFT JOIN FETCH c.clusterDefinition "
            + "LEFT JOIN FETCH c.hostGroups hg LEFT JOIN FETCH hg.hostMetadata "
            + "LEFT JOIN FETCH s.credential LEFT JOIN FETCH s.stackStatus LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "LEFT JOIN FETCH s.userView LEFT JOIN FETCH s.environment LEFT JOIN FETCH c.kerberosConfig "
            + "WHERE s.workspace.id= :id AND s.terminated = null "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Set<StackApiView> findByWorkspaceId(@Param("id") Long id);

    @CheckPermissionsByWorkspaceId
    @Query("SELECT s FROM StackApiView s LEFT JOIN FETCH s.cluster c LEFT JOIN FETCH c.clusterDefinition "
            + "LEFT JOIN FETCH c.hostGroups hg LEFT JOIN FETCH hg.hostMetadata "
            + "LEFT JOIN FETCH s.credential LEFT JOIN FETCH s.stackStatus LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "LEFT JOIN FETCH s.userView LEFT JOIN FETCH s.environment LEFT JOIN FETCH c.kerberosConfig "
            + "WHERE s.workspace.id= :id AND :environment in e AND s.terminated = null "
            + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Set<StackApiView> findAllByWorkspaceIdAndEnvironments(@Param("id") Long id, @Param("environment") EnvironmentView environment);
}