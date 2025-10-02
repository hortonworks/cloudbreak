package com.sequenceiq.periscope.repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.domain.ScalingActivity;

@EntityType(entityClass = ScalingActivity.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface ScalingActivityRepository extends PagingAndSortingRepository<ScalingActivity, Long>, CrudRepository<ScalingActivity, Long> {

    @Query("SELECT st FROM ScalingActivity st WHERE st.operationId = :operationId AND st.cluster.stackCrn = :clusterCrn")
    Optional<ScalingActivity> findByOperationIdAndClusterCrn(@Param("operationId") String operationId, @Param("clusterCrn") String clusterCrn);

    @Query("SELECT st FROM ScalingActivity st WHERE st.operationId = :operationId AND st.cluster.stackName = :clusterName")
    Optional<ScalingActivity> findByOperationIdAndClusterName(@Param("operationId") String operationId, @Param("clusterName") String clusterName);

    @Query("SELECT st FROM ScalingActivity st WHERE st.cluster.stackName = :clusterName")
    List<ScalingActivity> findAllByCluster(@Param("clusterName") String clusterName);

    @Query("SELECT st FROM ScalingActivity st WHERE st.cluster.id = :clusterId AND st.startTime < :startTimeBefore ORDER BY st.startTime DESC")
    List<ScalingActivity> findAllByClusterWithStartTimeBefore(@Param("clusterId") Long clusterId, @Param("startTimeBefore") Date startTimeBefore);

    @Query("SELECT st FROM ScalingActivity st WHERE st.cluster.stackName = :clusterName" +
            " AND st.startTime >= :startTimeAfter ORDER BY st.startTime DESC")
    Page<ScalingActivity> findAllByClusterNameWithStartTimeAfter(@Param("clusterName") String clusterName,
            @Param("startTimeAfter") Date startTimeAfter, Pageable pageable);

    @Query("SELECT st FROM ScalingActivity st WHERE st.cluster.stackCrn = :clusterCrn" +
            " AND st.startTime >= :startTimeAfter ORDER BY st.startTime DESC")
    Page<ScalingActivity> findAllByClusterCrnWithStartTimeAfter(@Param("clusterCrn") String clusterCrn,
            @Param("startTimeAfter") Date startTimeAfter, Pageable pageable);

    @Query("SELECT st FROM ScalingActivity st WHERE st.cluster.id = :clusterId AND st.activityStatus = :activityStatus AND st.startTime >= :startTimeFrom " +
            "AND st.startTime < :startTimeUntil ORDER BY st.startTime DESC")
    List<ScalingActivity> findAllByClusterAndActivityStatusBetweenInterval(@Param("clusterId") Long clusterId,
            @Param("activityStatus") ActivityStatus activityStatus, @Param("startTimeFrom") Date startTimeFrom, @Param("startTimeUntil") Date startTimeUntil);

    @Query("SELECT st FROM ScalingActivity st WHERE st.cluster.stackName = :clusterName" +
            " AND st.activityStatus IN :statuses AND st.startTime >= :startTimeAfter ORDER BY st.startTime DESC")
    Page<ScalingActivity> findAllByClusterNameAndInStatusesWithTimeAfter(@Param("clusterName") String clusterName,
            @Param("statuses") Collection<ActivityStatus> statuses, @Param("startTimeAfter") Date startTimeAfter, Pageable pageable);

    @Query("SELECT st FROM ScalingActivity st WHERE st.cluster.stackCrn = :clusterCrn" +
            " AND st.activityStatus IN :statuses AND st.startTime >= :startTimeAfter ORDER BY st.startTime DESC")
    Page<ScalingActivity> findAllByClusterCrnAndInStatusesWithTimeAfter(@Param("clusterCrn") String clusterCrn,
            @Param("statuses") Collection<ActivityStatus> statuses, @Param("startTimeAfter") Date startTimeAfter, Pageable pageable);

    @Query("SELECT st FROM ScalingActivity st WHERE st.cluster.id = :clusterId AND st.activityStatus IN :statuses")
    List<ScalingActivity> findAllByClusterAndInStatuses(@Param("clusterId") Long clusterId, @Param("statuses") Collection<ActivityStatus> statuses);

    @Query("SELECT st FROM ScalingActivity st WHERE st.cluster.stackName = :clusterName AND st.activityStatus IN :statuses " +
            "AND st.startTime >= :startTimeFrom AND st.startTime < :startTimeUntil ORDER BY st.startTime DESC")
    Page<ScalingActivity> findAllByClusterNameAndInStatusesBetweenInterval(@Param("clusterName") String clusterName,
            @Param("statuses") Collection<ActivityStatus> statuses, @Param("startTimeFrom") Date startTimeFrom,
            @Param("startTimeUntil") Date startTimeUntil, Pageable pageable);

    @Query("SELECT st FROM ScalingActivity st WHERE st.cluster.stackCrn = :clusterCrn AND st.activityStatus IN :statuses " +
            "AND st.startTime >= :startTimeFrom AND st.startTime < :startTimeUntil ORDER BY st.startTime DESC")
    Page<ScalingActivity> findAllByClusterCrnAndInStatusesBetweenInterval(@Param("clusterCrn") String clusterCrn,
            @Param("statuses") Collection<ActivityStatus> statuses, @Param("startTimeFrom") Date startTimeFrom,
            @Param("startTimeUntil") Date startTimeUntil, Pageable pageable);

    @Query("SELECT st FROM ScalingActivity st WHERE st.cluster.stackName = :clusterName " +
            "AND st.startTime >= :startTimeFrom AND st.startTime < :startTimeUntil ORDER BY st.startTime DESC")
    Page<ScalingActivity> findAllByClusterNameBetweenInterval(@Param("clusterName") String clusterName, @Param("startTimeFrom") Date startTimeFrom,
            @Param("startTimeUntil") Date startTimeUntil, Pageable pageable);

    @Query("SELECT st FROM ScalingActivity st WHERE st.cluster.stackCrn = :clusterCrn " +
            "AND st.startTime >= :startTimeFrom AND st.startTime < :startTimeUntil ORDER BY st.startTime DESC")
    Page<ScalingActivity> findAllByClusterCrnBetweenInterval(@Param("clusterCrn") String clusterCrn, @Param("startTimeFrom") Date startTimeFrom,
            @Param("startTimeUntil") Date startTimeUntil, Pageable pageable);

    @Query("SELECT st.id FROM ScalingActivity st WHERE st.endTime <= :endTimeBefore")
    List<Long> findAllIdsWithEndTimeBefore(@Param("endTimeBefore") Date endTimeBefore);

    @Query("SELECT st.id FROM ScalingActivity st WHERE st.activityStatus IN :statuses AND st.startTime <= :startTimeBefore")
    List<Long> findAllIdsInActivityStatusesWithStartTimeBefore(@Param("statuses") Collection<ActivityStatus> statuses,
            @Param("startTimeBefore") Date startTimeBefore, Pageable pageable);

    @Query("SELECT st.id FROM ScalingActivity st WHERE st.activityStatus IN :statuses")
    List<Long> findAllIdsInActivityStatuses(@Param("statuses") List<ActivityStatus> statuses, Sort sort);

    @Query("SELECT count(st) FROM ScalingActivity st WHERE st.activityStatus IN :statuses")
    Long countAllInActivityStatuses(@Param("statuses") Collection<ActivityStatus> statuses);

    @Modifying
    @Query("DELETE FROM ScalingActivity st WHERE st.cluster.id = :clusterId")
    void deleteAllByCluster(@Param("clusterId") Long clusterId);

    @Modifying
    @Query("UPDATE ScalingActivity st SET st.activityStatus = :status WHERE st.id IN :ids")
    void setActivityStatusesInIds(@Param("ids") Collection<Long> ids, @Param("status") ActivityStatus status);

    @Modifying
    @Query("UPDATE ScalingActivity st SET st.endTime = :endTime WHERE st.id = :id")
    void setEndTimeForScalingActivity(@Param("id") Long id, @Param("endTime") Date endTime);

    @Modifying
    @Query("UPDATE ScalingActivity st SET st.activityStatus = :status, st.scalingActivityReason = :message WHERE st.id IN :ids")
    void setActivityStatusAndReasonInIds(@Param("ids") Collection<Long> ids, @Param("status") ActivityStatus status, @Param("message") String message);
}
