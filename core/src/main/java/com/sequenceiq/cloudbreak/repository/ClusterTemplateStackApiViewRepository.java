package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateStackApiView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = StackView.class)
@Transactional(TxType.REQUIRED)
public interface ClusterTemplateStackApiViewRepository extends WorkspaceResourceRepository<ClusterTemplateStackApiView, Long> {

    @Query("SELECT s FROM ClusterTemplateStackApiView s "
            + "LEFT JOIN FETCH s.cluster c "
            + "LEFT JOIN FETCH c.blueprint "
            + "LEFT JOIN FETCH s.instanceGroups ig "
            + "WHERE s.id= :id")
    Optional<ClusterTemplateStackApiView> findById(@Param("id") Long id);

    @Query("SELECT s FROM ClusterTemplateStackApiView s "
            + "LEFT JOIN FETCH s.cluster c "
            + "LEFT JOIN FETCH c.blueprint "
            + "LEFT JOIN FETCH s.instanceGroups ig "
            + "WHERE s.resourceCrn= :crn AND s.type = :type")
    Optional<ClusterTemplateStackApiView> findByResourceCrnAndStackType(@Param("crn") String crn, @Param("type") StackType type);
}
