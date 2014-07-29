package com.sequenceiq.cloudbreak.service.history.converter;

import com.sequenceiq.cloudbreak.domain.AbstractHistory;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

public interface HistoryConverter<T extends ProvisionEntity, E extends AbstractHistory> {
    E convert(T entity);

    boolean supportsEntity(ProvisionEntity entity);
}
