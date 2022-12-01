package com.sequenceiq.periscope.repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.api.model.TriggerStatus;
import com.sequenceiq.periscope.domain.ScalingTrigger;

@EntityType(entityClass = ScalingTrigger.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface ScalingTriggerRepository extends CrudRepository<ScalingTrigger, Long> {

    @Query("SELECT st FROM ScalingTrigger st WHERE st.triggerCrn = :triggerCrn")
    Optional<ScalingTrigger> findByTriggerCrn(@Param("triggerCrn") String triggerCrn);

    @Query("SELECT st FROM ScalingTrigger st WHERE st.cluster.id = :clusterId")
    List<ScalingTrigger> findAllByCluster(@Param("clusterId") Long clusterId);

    @Query("SELECT st FROM ScalingTrigger st WHERE st.cluster.id = :clusterId AND st.startTime <= :startTimeBefore")
    List<ScalingTrigger> findAllByClusterWithStartTimeBefore(@Param("clusterId") Long clusterId, @Param("startTimeBefore") Date startTimeBefore);

    @Query("SELECT st FROM ScalingTrigger st WHERE st.cluster.id = :clusterId AND st.triggerStatus IN :statuses AND st.startTime >= :startTimeAfter")
    List<ScalingTrigger> findAllByClusterAndTriggerStatuses(@Param("clusterId") Long clusterId, @Param("statuses") Collection<TriggerStatus> statuses, @Param("startTimeAfter") Date startTimeAfter);


    @Query("SELECT st FROM ScalingTrigger st WHERE st.cluster.id = :clusterId AND st.startTime >= :startTimeAfter")
    List<ScalingTrigger> findAllByClusterWithStartTimeAfter(@Param("clusterId") Long clusterId, @Param("startTimeAfter") Date startTimeAfter);

    @Query("SELECT st FROM ScalingTrigger st WHERE st.cluster.id = :clusterId AND st.triggerStatus = :triggerStatus AND st.startTime >= :startTimeFrom " +
            "AND st.startTime <= :startTimeUntil")
    List<ScalingTrigger> findAllByClusterAndTriggerStatusBetweenInterval(@Param("clusterId") Long clusterId,
            @Param("triggerStatus") TriggerStatus triggerStatus, @Param("startTimeFrom") Date startTimeFrom, @Param("startTimeUntil") Date startTimeUntil);

    @Query("SELECT st FROM ScalingTrigger st WHERE st.cluster.id = :clusterId AND st.startTime >= :startTimeFrom " +
            "AND st.startTime <= :startTimeUntil")
    List<ScalingTrigger> findAllByClusterInGivenInterval(@Param("clusterId") Long clusterId
            , @Param("startTimeFrom") Date startTimeFrom, @Param("startTimeUntil") Date startTimeUntil);

    @Query("SELECT st FROM ScalingTrigger st WHERE st.cluster.id = :clusterId AND st.triggerStatus IN :statuses")
    List<ScalingTrigger> findAllByClusterAndInTriggerStatuses(@Param("clusterId") Long clusterId, @Param("statuses") Collection<TriggerStatus> statuses);

    @Query("SELECT st FROM ScalingTrigger st WHERE st.cluster.id = :clusterId AND st.startTime >= :startTimeFrom " +
            "AND st.startTime <= :startTimeUntil")
    List<ScalingTrigger> findAllByClusterBetweenInterval(@Param("clusterId") Long clusterId, @Param("startTimeFrom") Date startTimeFrom,
            @Param("startTimeUntil") Date startTimeUntil);
}
