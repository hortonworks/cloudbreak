package com.sequenceiq.distrox.v1.distrox.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.repository.ClouderaManagerRepositoryV1Request;

@Component
public class ClouderaManagerRepositoryV1ToClouderaManagerRepositoryV4Converter {

    public ClouderaManagerRepositoryV4Request convert(ClouderaManagerRepositoryV1Request source) {
        ClouderaManagerRepositoryV4Request response = new ClouderaManagerRepositoryV4Request();
        response.setBaseUrl(source.getBaseUrl());
        response.setVersion(source.getVersion());
        response.setGpgKeyUrl(source.getGpgKeyUrl());
        return response;
    }
}
