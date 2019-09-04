package com.sequenceiq.cloudbreak.repository.cluster;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplateView;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = ClusterTemplateView.class)
@Transactional(TxType.REQUIRED)
public interface ClusterTemplateViewRepository extends WorkspaceResourceRepository<ClusterTemplateView, Long> {

    @Query("SELECT b FROM ClusterTemplateView b WHERE b.workspace.id= :workspaceId AND b.status <> 'DEFAULT_DELETED'")
    Set<ClusterTemplateView> findAllActive(@Param("workspaceId") Long workspaceId);

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
