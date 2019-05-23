package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.ifNotNullF;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.ClouderaManagerV1Request;

@Component
public class ClouderaManagerV1ToClouderaManagerV4Converter {

    @Inject
    private ClouderaManagerProductV1ToClouderaManagerProductV4Converter clouderaManagerProductConverter;

    @Inject
    private ClouderaManagerRepositoryV1ToClouderaManagerRepositoryV4Converter repositoryConverter;

    public ClouderaManagerV4Request convert(ClouderaManagerV1Request source) {
        ClouderaManagerV4Request response = new ClouderaManagerV4Request();
        response.setProducts(ifNotNullF(source.getProducts(), clouderaManagerProductConverter::convert));
        response.setRepository(ifNotNullF(source.getRepository(), repositoryConverter::convert));
        return response;
    }
}
