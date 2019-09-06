package com.sequenceiq.cloudbreak.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.workspace.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@Transactional(TxType.REQUIRED)
@EntityType(entityClass = Cluster.class)
@WorkspaceResourceType(resource = WorkspaceResource.STACK)
public interface ClusterRepository extends WorkspaceResourceRepository<Cluster, Long> {

    @Override
    @DisableCheckPermissions
    Cluster save(Cluster entity);

    @CheckPermissionsByReturnValue
    Cluster findOneByStackId(long stackId);

    @CheckPermissionsByReturnValue
    @Query("SELECT c FROM Cluster c LEFT JOIN FETCH c.hostGroups LEFT JOIN FETCH c.containers LEFT JOIN FETCH c.components "
            + "LEFT JOIN FETCH c.rdsConfigs WHERE c.id= :id")
    Cluster findOneWithLists(@Param("id") Long id);

    @CheckPermissionsByReturnValue
    @Query("SELECT c FROM Cluster c WHERE c.status IN :statuses")
    List<Cluster> findByStatuses(@Param("statuses") Collection<Status> statuses);

    @CheckPermissionsByReturnValue
    @Query("SELECT c FROM Cluster c inner join c.rdsConfigs rc WHERE rc.id= :id")
    Set<Cluster> findAllClustersByRDSConfig(@Param("id") Long rdsConfigId);

    @CheckPermissionsByReturnValue
    @Query("SELECT c FROM Cluster c LEFT JOIN FETCH c.stack WHERE c.workspace = null")
    Set<Cluster> findAllWithNoWorkspace();

    @CheckPermissionsByReturnValue
    List<Cluster> findByLdapConfig(LdapConfig ldapConfig);

    @CheckPermissionsByReturnValue
    Set<Cluster> findByBlueprint(Blueprint blueprint);

    @CheckPermissionsByReturnValue
    Set<Cluster> findByProxyConfig(ProxyConfig proxyConfig);
}