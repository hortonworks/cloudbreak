package com.sequenceiq.periscope.repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.domain.ScalingActivity;

@EntityType(entityClass = ScalingActivity.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface ScalingActivityRepository extends CrudRepository<ScalingActivity, Long> {

    @Query("SELECT st FROM ScalingActivity st WHERE st.activityCrn = :activityCrn")
    Optional<ScalingActivity> findByActivityCrn(@Param("activityCrn") String activityCrn);

    @Query("SELECT st FROM ScalingActivity st WHERE st.cluster.id = :clusterId")
    List<ScalingActivity> findAllByCluster(@Param("clusterId") Long clusterId);

    @Query("SELECT st FROM ScalingActivity st WHERE st.cluster.id = :clusterId AND st.startTime < :startTimeBefore ORDER BY st.startTime DESC")
    List<ScalingActivity> findAllByClusterWithStartTimeBefore(@Param("clusterId") Long clusterId, @Param("startTimeBefore") Date startTimeBefore);

    @Query("SELECT st FROM ScalingActivity st WHERE st.cluster.id = :clusterId AND st.startTime >= :startTimeAfter ORDER BY st.startTime DESC")
    List<ScalingActivity> findAllByClusterWithStartTimeAfter(@Param("clusterId") Long clusterId, @Param("startTimeAfter") Date startTimeAfter);

    @Query("SELECT st FROM ScalingActivity st WHERE st.cluster.id = :clusterId AND st.activityStatus = :activityStatus AND st.startTime >= :startTimeFrom " +
            "AND st.startTime < :startTimeUntil ORDER BY st.startTime DESC")
    List<ScalingActivity> findAllByClusterAndActivityStatusBetweenInterval(@Param("clusterId") Long clusterId,
            @Param("activityStatus") ActivityStatus activityStatus, @Param("startTimeFrom") Date startTimeFrom, @Param("startTimeUntil") Date startTimeUntil);

    @Query("SELECT st FROM ScalingActivity st WHERE st.cluster.id = :clusterId AND st.activityStatus IN :statuses")
    List<ScalingActivity> findAllByClusterAndInStatuses(@Param("clusterId") Long clusterId, @Param("statuses") Collection<ActivityStatus> statuses);

    @Query("SELECT st FROM ScalingActivity st WHERE st.cluster.id = :clusterId AND st.startTime >= :startTimeFrom " +
            "AND st.startTime < :startTimeUntil ORDER BY st.startTime DESC")
    List<ScalingActivity> findAllByClusterBetweenInterval(@Param("clusterId") Long clusterId, @Param("startTimeFrom") Date startTimeFrom,
            @Param("startTimeUntil") Date startTimeUntil);

    @Query("SELECT st.id FROM ScalingActivity st WHERE st.endTime <= :endTimeBefore")
    List<Long> findAllIdsWithEndTimeBefore(@Param("endTimeBefore") Date endTimeBefore);

    @Query("SELECT st.id FROM ScalingActivity st WHERE st.activityStatus IN :statuses AND st.startTime <= :startTimeBefore")
    List<Long> findAllIdsInActivityStatusesWithStartTimeBefore(@Param("statuses") Collection<ActivityStatus> statuses,
            @Param("startTimeBefore") Date startTimeBefore);

    @Modifying
    @Query("DELETE FROM ScalingActivity st WHERE st.cluster.id = :clusterId")
    void deleteAllByCluster(@Param("clusterId") Long clusterId);
}
