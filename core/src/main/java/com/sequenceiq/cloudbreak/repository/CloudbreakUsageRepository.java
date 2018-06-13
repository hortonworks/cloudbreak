package com.sequenceiq.cloudbreak.repository;

import java.util.Date;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;

@EntityType(entityClass = CloudbreakUsage.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface CloudbreakUsageRepository extends CrudRepository<CloudbreakUsage, Long>, JpaSpecificationExecutor {

    @Query("SELECT u FROM CloudbreakUsage u WHERE u.stackId = :stackId AND u.status = 'OPEN'")
    List<CloudbreakUsage> findOpensForStack(@Param("stackId") Long stackId);

    @Query("SELECT u FROM CloudbreakUsage u WHERE u.stackId = :stackId AND u.status = 'STOPPED'")
    List<CloudbreakUsage> findStoppedForStack(@Param("stackId") Long stackId);

    @Query("SELECT u FROM CloudbreakUsage u WHERE u.stackId = :stackId AND u.instanceGroup = :instanceGroupName AND u.status = 'OPEN'")
    CloudbreakUsage getOpenUsageByStackAndGroupName(@Param("stackId") Long stackId, @Param("instanceGroupName") String instanceGroupName);

    @Query("SELECT u FROM CloudbreakUsage u WHERE (u.status = 'STOPPED' OR u.status = 'OPEN')AND u.day < :today")
    List<CloudbreakUsage> findAllOpenAndStopped(@Param("today") Date today);
}
