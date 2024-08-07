package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = StackApiView.class)
@Transactional(TxType.REQUIRED)
public interface StackApiViewRepository extends WorkspaceResourceRepository<StackApiView, Long> {

    @Query("SELECT s FROM StackApiView s LEFT JOIN FETCH s.cluster c LEFT JOIN FETCH c.blueprint "
            + "LEFT JOIN FETCH c.hostGroups hg"
            + "LEFT JOIN FETCH s.stackStatus LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "LEFT JOIN FETCH s.userView LEFT JOIN FETCH s.workspace w LEFT JOIN FETCH w.tenant WHERE s.id= :id")
    Optional<StackApiView> findById(@Param("id") Long id);

    @Query("SELECT s FROM StackApiView s LEFT JOIN FETCH s.cluster c LEFT JOIN FETCH c.blueprint "
            + "LEFT JOIN FETCH c.hostGroups hg"
            + "LEFT JOIN FETCH s.stackStatus LEFT JOIN FETCH s.instanceGroups ig LEFT JOIN FETCH ig.instanceMetaData "
            + "LEFT JOIN FETCH s.userView LEFT JOIN FETCH s.workspace w LEFT JOIN FETCH w.tenant WHERE s.resourceCrn= :crn AND s.type = :type")
    Optional<StackApiView> findByResourceCrnAndStackType(@Param("crn") String crn, @Param("type") StackType type);

}
