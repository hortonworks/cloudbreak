package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;

@EntityType(entityClass = CloudbreakEvent.class)
public interface CloudbreakEventRepository extends PagingAndSortingRepository<CloudbreakEvent, Long>, JpaSpecificationExecutor {

    @Query("SELECT cbe FROM CloudbreakEvent cbe WHERE cbe.stackId= :stackId AND cbe.owner= :owner")
    List<CloudbreakEvent> findCloudbreakEventsForStack(@Param("owner") String owner, @Param("stackId") Long stackId);

}
