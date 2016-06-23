package com.sequenceiq.cloudbreak.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

import com.sequenceiq.cloudbreak.domain.FlowLog

@EntityType(entityClass = FlowLog::class)
interface FlowLogRepository : CrudRepository<FlowLog, Long> {

    @Query("SELECT DISTINCT fl.flowId FROM FlowLog fl "
            + "WHERE (fl.finalized IS NULL OR fl.finalized = false) AND fl.stackId = :stackId "
            + "AND fl.flowType != 'com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig'")
    fun findAllRunningNonTerminationFlowIdsByStackId(@Param("stackId") stackId: Long?): Set<String>

    @Query("SELECT DISTINCT fl.flowId, fl.stackId FROM FlowLog fl WHERE fl.finalized IS NULL OR fl.finalized = false")
    fun findAllNonFinalized(): List<Array<Any>>

    @Modifying
    @Query("UPDATE FlowLog fl SET fl.finalized = true WHERE fl.flowId = :flowId")
    fun finalizeByFlowId(@Param("flowId") flowId: String)
}
