package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.repository.snippets.ShowTerminatedClustersSnippets.SHOW_TERMINATED_CLUSTERS_IF_REQUESTED;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = StackApiView.class)
@Transactional(TxType.REQUIRED)
public interface StackApiViewRepository extends WorkspaceResourceRepository<StackApiView, Long> {

    @Query("SELECT s FROM StackApiView s LEFT JOIN FETCH s.cluster c LEFT JOIN FETCH c.blueprint "
            + "LEFT JOIN FETCH c.hostGroups hg LEFT JOIN FETCH hg.hostMetadata "
            + "LEFT JOIN FETCH s.stackStatus LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "LEFT JOIN FETCH s.userView LEFT JOIN FETCH s.workspace w LEFT JOIN FETCH w.tenant WHERE s.id= :id")
    Optional<StackApiView> findById(@Param("id") Long id);

    @Query("SELECT s FROM StackApiView s LEFT JOIN FETCH s.cluster c LEFT JOIN FETCH c.blueprint "
            + "LEFT JOIN FETCH c.hostGroups hg LEFT JOIN FETCH hg.hostMetadata "
            + "LEFT JOIN FETCH s.stackStatus LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "LEFT JOIN FETCH s.userView LEFT JOIN FETCH s.workspace w LEFT JOIN FETCH w.tenant WHERE s.workspace.id= :id AND "
            + SHOW_TERMINATED_CLUSTERS_IF_REQUESTED + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Set<StackApiView> findAllByWorkspaceId(
            @Param("id") Long id,
            @Param("showTerminated") Boolean showTerminated,
            @Param("terminatedAfter") Long terminatedAfter
    );

    @Query("SELECT s FROM StackApiView s LEFT JOIN FETCH s.cluster c LEFT JOIN FETCH c.blueprint "
            + "LEFT JOIN FETCH c.hostGroups hg LEFT JOIN FETCH hg.hostMetadata "
            + "LEFT JOIN FETCH s.stackStatus LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "LEFT JOIN FETCH s.userView LEFT JOIN FETCH s.workspace w LEFT JOIN FETCH w.tenant WHERE s.workspace.id = :id AND "
            + "s.environmentCrn = :environmentCrn AND " + SHOW_TERMINATED_CLUSTERS_IF_REQUESTED + "AND (s.type is not 'TEMPLATE' OR s.type is null)")
    Set<StackApiView> findAllByWorkspaceIdAndEnvironments(
            @Param("id") Long id,
            @Param("environmentCrn") String environmentCrn,
            @Param("showTerminated") Boolean showTerminated,
            @Param("terminatedAfter") Long terminatedAfter
    );
}
