package com.sequenceiq.cloudbreak.repository;

import com.sequenceiq.cloudbreak.domain.TemplateHistory;
import org.springframework.data.repository.CrudRepository;

public interface TemplateHistoryRepository extends CrudRepository<TemplateHistory, Long> {
}
