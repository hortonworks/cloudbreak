package com.sequenceiq.cloudbreak.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;

@EntityType(entityClass = CloudbreakUsage.class)
public interface CloudbreakUsageRepository extends CrudRepository<CloudbreakUsage, Long>, JpaSpecificationExecutor {

    List<CloudbreakUsage> findOpensForStack(@Param("stackId") Long stackId);

    List<CloudbreakUsage> findStoppedForStack(@Param("stackId") Long stackId);

    CloudbreakUsage getOpenUsageByStackAndGroupName(@Param("stackId") Long stackId, @Param("instanceGroupName") String instanceGroupName);

    List<CloudbreakUsage> findAllOpenAndStopped(@Param("today") Date today);
}
