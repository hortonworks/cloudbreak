package com.sequenceiq.cloudbreak.repository;

import java.util.Date;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beust.jcommander.internal.Nullable;
import com.sequenceiq.cloudbreak.aspect.BaseRepository;
import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = CloudbreakUsage.class)
@Transactional(Transactional.TxType.REQUIRED)
@HasPermission
public interface CloudbreakUsageRepository extends BaseRepository<CloudbreakUsage, Long>, JpaSpecificationExecutor<CloudbreakUsage> {

    @Query("SELECT u FROM CloudbreakUsage u WHERE u.stackId = :stackId AND u.status = 'OPEN'")
    List<CloudbreakUsage> findOpensForStack(@Param("stackId") Long stackId);

    @Query("SELECT u FROM CloudbreakUsage u WHERE u.stackId = :stackId AND u.status = 'STOPPED'")
    List<CloudbreakUsage> findStoppedForStack(@Param("stackId") Long stackId);

    @Query("SELECT u FROM CloudbreakUsage u WHERE u.stackId = :stackId AND u.instanceGroup = :instanceGroupName AND u.status = 'OPEN'")
    CloudbreakUsage getOpenUsageByStackAndGroupName(@Param("stackId") Long stackId, @Param("instanceGroupName") String instanceGroupName);

    @Query("SELECT u FROM CloudbreakUsage u WHERE (u.status = 'STOPPED' OR u.status = 'OPEN')AND u.day < :today")
    List<CloudbreakUsage> findAllOpenAndStopped(@Param("today") Date today);

    @Override
    List<CloudbreakUsage> findAll(@Nullable Specification<CloudbreakUsage> spec);
}
