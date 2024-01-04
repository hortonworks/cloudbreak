package com.sequenceiq.flow.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.flow.domain.FlowChainLog;

@Transactional(TxType.REQUIRED)
public interface FlowChainLogRepository extends CrudRepository<FlowChainLog, Long> {

    String FIND_BY_FLOW_CHAIN_ID_BASE_QUERY = "FROM FlowChainLog fcl " +
            "INNER JOIN ( " +
            "    SELECT flowChainId, max(created) as created " +
            "    FROM FlowChainLog " +
            "    WHERE flowChainId IN (:flowChainIds) " +
            "    GROUP by flowChainId " +
            ")fch on fcl.flowChainId = fch.flowChainId and fcl.created = fch.created";

    List<FlowChainLog> findByParentFlowChainIdOrderByCreatedDesc(String parentFlowChainId);

    List<FlowChainLog> findByFlowChainIdOrderByCreatedDesc(String flowChainId);

    Optional<FlowChainLog> findFirstByFlowChainIdOrderByCreatedDesc(String flowChainId);

    Optional<FlowChainLog> findFirstByFlowChainIdOrderByCreatedAsc(String flowChainId);

    @Modifying
    @Query("DELETE FROM FlowChainLog fch "
            + "WHERE fch.flowChainId NOT IN ( SELECT DISTINCT fl.flowChainId FROM FlowLog fl )"
            + " AND fch.flowChainId NOT IN (SELECT DISTINCT fc.parentFlowChainId FROM FlowChainLog fc)")
    int purgeOrphanFlowChainLogs();

    @Query(value = "SELECT fcl.* " + FIND_BY_FLOW_CHAIN_ID_BASE_QUERY, nativeQuery = true,
            countQuery = "SELECT count(fcl.*) " + FIND_BY_FLOW_CHAIN_ID_BASE_QUERY)
    Page<FlowChainLog> nativeFindByFlowChainIdInOrderByCreatedDesc(@Param("flowChainIds") Set<String> flowChainIds, Pageable pageable);
}
