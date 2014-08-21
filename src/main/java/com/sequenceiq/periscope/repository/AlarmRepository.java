package com.sequenceiq.periscope.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.periscope.domain.Alarm;

public interface AlarmRepository extends CrudRepository<Alarm, Long> {
}
