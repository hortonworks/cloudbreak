package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.domain.TemplateHistory;

public interface TemplateHistoryRepository extends CrudRepository<TemplateHistory, Long> {
}
