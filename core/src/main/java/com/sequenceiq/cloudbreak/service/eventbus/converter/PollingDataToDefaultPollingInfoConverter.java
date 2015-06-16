package com.sequenceiq.cloudbreak.service.eventbus.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.polling.DummyPollingInfo;
import com.sequenceiq.cloudbreak.cloud.polling.NumericPollingReference;
import com.sequenceiq.cloudbreak.cloud.polling.PollingInfo;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.PollingData;

@Component
public class PollingDataToDefaultPollingInfoConverter extends AbstractConversionServiceAwareConverter<PollingData, DummyPollingInfo> {

    @Override
    public DummyPollingInfo convert(PollingData source) {
        DummyPollingInfo dummyPollingInfo = new DummyPollingInfo();
        dummyPollingInfo.setPollingStatus(PollingInfo.PollingStatus.valueOf(source.getStatus()));
        dummyPollingInfo.setPollingReference(new NumericPollingReference(source.getId()));
        dummyPollingInfo.setPollingCount(source.getNumberOfPolls());
        return dummyPollingInfo;
    }
}
