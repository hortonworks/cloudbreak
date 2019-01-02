package com.sequenceiq.cloudbreak.repository.cluster;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.workspace.WorkspaceResourceType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplateView;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = ClusterTemplateView.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisableHasPermission
@WorkspaceResourceType(resource = WorkspaceResource.CLUSTER_TEMPLATE)
public interface ClusterTemplateViewRepository extends WorkspaceResourceRepository<ClusterTemplateView, Long> {

    @Override
    default <S extends ClusterTemplateView> S save(S entity) {
        throw new UnsupportedOperationException("Creation is not supported from ClusterTemplateViewRepository");
    }

    @Override
    default <S extends ClusterTemplateView> Iterable<S> saveAll(Iterable<S> entities) {
        throw new UnsupportedOperationException("Creation is not supported from ClusterTemplateViewRepository");
    }

    @Override
    default void delete(ClusterTemplateView entity) {
        throw new UnsupportedOperationException("Deletion is not supported from ClusterTemplateViewRepository");
    }

    @Override
    default void deleteById(Long id) {
        throw new UnsupportedOperationException("Deletion is not supported from ClusterTemplateViewRepository");
    }

    @Override
    default void deleteAll(Iterable<? extends ClusterTemplateView> entities) {
        throw new UnsupportedOperationException("Deletion is not supported from ClusterTemplateViewRepository");
    }

    @Override
    default void deleteAll() {
        throw new UnsupportedOperationException("Deletion is not supported from ClusterTemplateViewRepository");
    }

}
