package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CloudbreakDetailsJson;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;

@Component
public class CloudbreakDetailsToJsonConverter extends AbstractConversionServiceAwareConverter<CloudbreakDetails, CloudbreakDetailsJson>  {

    @Override
    public CloudbreakDetailsJson convert(CloudbreakDetails source) {
        CloudbreakDetailsJson json = new CloudbreakDetailsJson();
        json.setVersion(source.getVersion());
        return json;
    }
}
