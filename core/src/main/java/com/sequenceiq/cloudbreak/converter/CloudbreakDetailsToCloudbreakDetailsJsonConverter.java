package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.CloudbreakDetailsJson;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import org.springframework.stereotype.Component;

@Component
public class CloudbreakDetailsToCloudbreakDetailsJsonConverter extends AbstractConversionServiceAwareConverter<CloudbreakDetails, CloudbreakDetailsJson>  {

    @Override
    public CloudbreakDetailsJson convert(CloudbreakDetails source) {
        CloudbreakDetailsJson json = new CloudbreakDetailsJson();
        json.setVersion(source.getVersion());
        return json;
    }
}
