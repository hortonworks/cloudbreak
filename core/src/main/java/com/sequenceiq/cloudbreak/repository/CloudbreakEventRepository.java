package com.sequenceiq.cloudbreak.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;

public interface CloudbreakEventRepository extends PagingAndSortingRepository<CloudbreakEvent, Long>, JpaSpecificationExecutor {

    List<CloudbreakEvent> cloudbreakEvents(@Param("owner") String owner);

    List<CloudbreakEvent> cloudbreakEventsSince(@Param("owner") String owner, @Param("since") Date since);

    List<CloudbreakEvent> findCloudbreakEventsForStack(@Param("stackId") Long stackId);

}
