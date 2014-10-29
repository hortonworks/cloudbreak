package com.sequenceiq.periscope.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.periscope.domain.MetricAlarm;

public interface MetricAlarmRepository extends CrudRepository<MetricAlarm, Long> {
}
