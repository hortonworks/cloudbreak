package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.domain.view.HostGroupView;
import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@DisableHasPermission
@EntityType(entityClass = HostGroupView.class)
@Transactional(TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.DATAHUB)
public interface HostGroupViewRepository extends DisabledBaseRepository<HostGroupView, Long> {

    @Query("SELECT h FROM HostGroupView h LEFT JOIN FETCH h.instanceGroup ig LEFT JOIN h.cluster c WHERE c.id IN :clusterIds")
    Set<HostGroupView> findHostGroupsInClusterList(@Param("clusterIds") Set<Long> clusterIds);
}
