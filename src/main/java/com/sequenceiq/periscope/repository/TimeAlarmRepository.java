package com.sequenceiq.periscope.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.periscope.domain.TimeAlarm;

public interface TimeAlarmRepository extends CrudRepository<TimeAlarm, Long> {
}
