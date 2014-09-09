package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;

public interface EventRepository extends CrudRepository<CloudbreakEvent, Long> {

}
