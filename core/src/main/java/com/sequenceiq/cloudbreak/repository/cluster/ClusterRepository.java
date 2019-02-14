package com.sequenceiq.cloudbreak.repository.cluster;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.workspace.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
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
    @Query("SELECT c FROM Cluster c inner join c.hostGroups hg WHERE hg.constraint.constraintTemplate.id = :id")
    List<Cluster> findAllClustersForConstraintTemplate(@Param("id") Long id);

    @CheckPermissionsByReturnValue
    @Query("SELECT c FROM Cluster c LEFT JOIN FETCH c.stack WHERE c.workspace = null")
    Set<Cluster> findAllWithNoWorkspace();

    @CheckPermissionsByReturnValue
    Set<Cluster> findByClusterDefinition(ClusterDefinition clusterDefinition);

    @CheckPermissionsByReturnValue
    Set<Cluster> findByLdapConfig(LdapConfig ldapConfig);

    @CheckPermissionsByReturnValue
    @Query("SELECT c FROM Cluster c WHERE c.environment.id = :environmentId AND c.ldapConfig = :ldapConfig")
    Set<Cluster> findByLdapConfigAndEnvironment(@Param("ldapConfig") LdapConfig ldapConfig, @Param("environmentId") Long environmentId);

    @CheckPermissionsByReturnValue
    Set<Cluster> findByProxyConfig(ProxyConfig proxyConfig);

    @CheckPermissionsByReturnValue
    @Query("SELECT c FROM Cluster c WHERE c.environment.id = :environmentId AND c.proxyConfig = :proxyConfig")
    Set<Cluster> findByProxyConfigAndEnvironment(@Param("proxyConfig") ProxyConfig proxyConfig, @Param("environmentId") Long environmentId);

    @CheckPermissionsByReturnValue
    @Query("SELECT c FROM Cluster c INNER JOIN c.rdsConfigs rc WHERE rc.id= :id")
    Set<Cluster> findByRdsConfig(@Param("id") Long rdsConfigId);

    @CheckPermissionsByReturnValue
    @Query("SELECT c FROM Cluster c INNER JOIN c.rdsConfigs rc WHERE c.environment.id = :environmentId AND rc.id= :id")
    Set<Cluster> findByRdsConfigAndEnvironment(@Param("id") Long rdsConfigId, @Param("environmentId") Long environmentId);

    @CheckPermissionsByReturnValue
    @Query("SELECT c FROM Cluster c INNER JOIN c.kerberosConfig kc WHERE kc.id= :id")
    Set<Cluster> findByKerberosConfig(@Param("id") Long kerberosConfigId);

    @CheckPermissionsByReturnValue
    @Query("SELECT c FROM Cluster c INNER JOIN c.kerberosConfig kc WHERE c.environment.id = :environmentId AND kc.id= :id")
    Set<Cluster> findByKerberosConfigAndEnvironment(@Param("id") Long id, @Param("environmentId") Long environmentId);
}