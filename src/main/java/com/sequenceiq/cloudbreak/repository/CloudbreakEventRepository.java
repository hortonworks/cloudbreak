package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;

public interface CloudbreakEventRepository extends CrudRepository<CloudbreakEvent, Long> {

    @Query("select * from CloudbreakEvent cbe where cbe.userId= :userid and cb.eventTimestamp> :since")
    List<CloudbreakEvent> cloudbreakEvents(@Param("userId") Long userId, @Param("since") long since);
}
