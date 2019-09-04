package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@EntityType(entityClass = StackView.class)
@Transactional(TxType.REQUIRED)
public interface StackViewRepository extends WorkspaceResourceRepository<StackView, Long> {

    @Query("SELECT s FROM StackView s WHERE s.id= :id")
    Optional<StackView> findById(@Param("id") Long id);

    @Query("SELECT s FROM StackView s WHERE s.workspace.id= :id AND s.terminated = null")
    Set<StackView> findByWorkspaceId(@Param("id") Long id);

    @Override
    default <S extends StackView> S save(S entity) {
        throw new UnsupportedOperationException("salala");
    }
}
