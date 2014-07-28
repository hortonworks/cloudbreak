package com.sequenceiq.cloudbreak.service.history;

import com.sequenceiq.cloudbreak.domain.HistoryEvent;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

public interface HistoryService<T extends ProvisionEntity> {

    void recordHistory(T entity, HistoryEvent historyEvent);

    void notify(T entity, HistoryEvent historyEvent);

    boolean isEntitySupported(T entity);

}
