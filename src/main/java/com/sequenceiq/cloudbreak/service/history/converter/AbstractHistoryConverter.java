package com.sequenceiq.cloudbreak.service.history.converter;

import com.sequenceiq.cloudbreak.domain.AbstractHistory;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

public abstract class AbstractHistoryConverter<T extends ProvisionEntity, E extends AbstractHistory> implements HistoryConverter<T, E> {
}
