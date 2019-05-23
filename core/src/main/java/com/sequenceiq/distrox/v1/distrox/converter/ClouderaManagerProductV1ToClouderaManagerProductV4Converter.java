package com.sequenceiq.distrox.v1.distrox.converter;

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.product.ClouderaManagerProductV1Request;

@Component
public class ClouderaManagerProductV1ToClouderaManagerProductV4Converter {

    public List<ClouderaManagerProductV4Request> convert(List<ClouderaManagerProductV1Request> source) {
        return source.stream().map(this::convert).collect(toList());
    }

    public ClouderaManagerProductV4Request convert(ClouderaManagerProductV1Request source) {
        ClouderaManagerProductV4Request response = new ClouderaManagerProductV4Request();
        response.setName(source.getName());
        response.setParcel(source.getParcel());
        response.setVersion(source.getVersion());
        return response;
    }
}
