package com.sequenceiq.cloudbreak.service.eventbus.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.polling.DummyPollingInfo;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.PollingData;

@Component
public class DefaultPollingInfoToPollingDataConverter extends AbstractConversionServiceAwareConverter<DummyPollingInfo, PollingData> {

    @Override
    public PollingData convert(DummyPollingInfo source) {
        PollingData pollingData = new PollingData();
        pollingData.setStatus(source.pollingStatus().toString());
        if (null != source.pollingReference()) {
            pollingData.setId(source.pollingReference().referenceData());
        }
        pollingData.setNumberOfPolls(source.pollingCount());
        return pollingData;
    }
}
