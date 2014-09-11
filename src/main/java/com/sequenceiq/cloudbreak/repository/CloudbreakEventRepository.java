package com.sequenceiq.cloudbreak.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;

public interface CloudbreakEventRepository extends CrudRepository<CloudbreakEvent, Long>, JpaSpecificationExecutor {

    List<CloudbreakEvent> cloudbreakEvents(@Param("user") String user);

    List<CloudbreakEvent> cloudbreakEventsSince(@Param("user") String user, @Param("since") Date since);

}
