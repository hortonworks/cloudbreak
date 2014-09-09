package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;

public interface CloudbreakEventRepository extends CrudRepository<CloudbreakEvent, Long> {

    @Query("select cbe from CloudbreakEvent cbe where (cbe.userId= :userId) and ( :since IS NULL or cbe.eventTimestamp > to_timestamp(:since))")
    List<CloudbreakEvent> cloudbreakEvents(@Param("userId") Long userId, @Param("since") Long since);
}
