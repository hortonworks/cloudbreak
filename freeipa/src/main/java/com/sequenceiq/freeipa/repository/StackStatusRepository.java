package com.sequenceiq.freeipa.repository;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.common.metrics.status.StackCountByStatusView;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.StackStatus;

@EntityType(entityClass = StackStatus.class)
@Transactional(TxType.REQUIRED)
public interface StackStatusRepository extends JpaRepository<StackStatus, Long> {

    List<StackStatus> findAllByStackIdOrderByCreatedDesc(long stackId);

    Optional<StackStatus> findFirstByStackIdOrderByCreatedDesc(long stackId);

    @Query("SELECT COUNT(status) as count, st.status as status " +
            "FROM StackStatus st " +
            "WHERE st.id IN (SELECT s.stackStatus.id FROM Stack s WHERE s.terminated < 0 AND s.cloudPlatform = :cloudPlatform) " +
            "GROUP BY (st.status)")
    List<StackCountByStatusView> countStacksByStatusAndCloudPlatform(@Param("cloudPlatform") String cloudPlatform);

    @Query("SELECT COUNT(status) as count, st.status as status " +
            "FROM StackStatus st " +
            "WHERE st.id IN (SELECT s.stackStatus.id FROM Stack s WHERE s.terminated < 0 AND s.tunnel = :tunnel) " +
            "GROUP BY (st.status)")
    List<StackCountByStatusView> countStacksByStatusAndTunnel(@Param("tunnel") Tunnel tunnel);

    @Modifying
    @Query("DELETE FROM StackStatus ss WHERE ss.stack.id = :stackId AND ss.status != :status")
    void deleteAllByStackIdAndStatusNot(long stackId, Status status);

    Page<StackStatus> findAllByCreatedLessThan(long timestampBefore, Pageable pageable);
}


