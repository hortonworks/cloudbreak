package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.storage.CloudStorageV1Request;

@Component
public class CloudStorageV1ToCloudStorageV4Converter {

    @Inject
    private LocationV4ToLocationV1Converter locationConverter;

    public CloudStorageV4Request convert(CloudStorageV1Request source) {
        CloudStorageV4Request response = new CloudStorageV4Request();
        response.setLocations(getIfNotNull(source.getLocations(), locationConverter::convertTo));
        response.setS3(source.getS3());
        response.setAdls(source.getAdls());
        response.setAdlsGen2(source.getAdlsGen2());
        response.setGcs(source.getGcs());
        response.setWasb(source.getWasb());
        return response;
    }

    public CloudStorageV1Request convert(CloudStorageV4Request source) {
        CloudStorageV1Request response = new CloudStorageV1Request();
        response.setLocations(getIfNotNull(source.getLocations(), locationConverter::convertFrom));
        response.setS3(source.getS3());
        response.setAdls(source.getAdls());
        response.setAdlsGen2(source.getAdlsGen2());
        response.setGcs(source.getGcs());
        response.setWasb(source.getWasb());
        return response;
    }
}
