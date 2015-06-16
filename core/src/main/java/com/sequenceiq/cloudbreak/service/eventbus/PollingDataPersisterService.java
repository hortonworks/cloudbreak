package com.sequenceiq.cloudbreak.service.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.model.DefaultPollingNotification;
import com.sequenceiq.cloudbreak.cloud.polling.DummyPollingInfo;
import com.sequenceiq.cloudbreak.domain.PollingData;

@Component
public class PollingDataPersisterService extends AbstractCloudPersisterService<DefaultPollingNotification> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingDataPersisterService.class);

    @Override
    public DefaultPollingNotification persist(DefaultPollingNotification data) {
        LOGGER.debug("Persisting polling notification data : {}", data);
        PollingData pollingData = getConversionService().convert(data.pollingInfo(), PollingData.class);
        pollingData = (PollingData) getRepositoryForEntity(pollingData).save(pollingData);
        DummyPollingInfo pollingInfo = getConversionService().convert(pollingData, DummyPollingInfo.class);
        return new DefaultPollingNotification(pollingInfo);
    }

    @Override
    public DefaultPollingNotification retrieve(DefaultPollingNotification data) {
        LOGGER.debug("Retrieving polling notification data : {}", data);
        PollingData pollingData = getConversionService().convert(data.pollingInfo(), PollingData.class);

        pollingData = (PollingData) getRepositoryForEntity(pollingData).findOne(pollingData.getId());
        DummyPollingInfo pollingInfo = getConversionService().convert(pollingData, DummyPollingInfo.class);
        return new DefaultPollingNotification(pollingInfo);
    }


}
