package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;

@EntityType(entityClass = CloudbreakEvent.class)
public interface CloudbreakEventRepository extends PagingAndSortingRepository<CloudbreakEvent, Long>, JpaSpecificationExecutor {

    List<CloudbreakEvent> findCloudbreakEventsForStack(@Param("stackId") Long stackId);

}
