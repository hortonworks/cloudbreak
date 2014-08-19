package com.sequenceiq.periscope.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.periscope.model.Alarm;

public interface AlarmRepository extends CrudRepository<Alarm, Long> {
}
