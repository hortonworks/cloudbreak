package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.Event;

public interface EventRepository extends CrudRepository<Event, Long> {

}
