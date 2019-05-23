package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.ifNotNullF;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.storage.CloudStorageV1Request;

@Component
public class CloudStorageV1ToCloudStorageV4Converter {

    @Inject
    private LocationV4ToLocationV1Converter locationConverter;

    @Inject
    private  CloudStorageConverter cloudStorageConverter;

    public CloudStorageV4Request convert(CloudStorageV1Request source) {
        CloudStorageV4Request response = new CloudStorageV4Request();
        response.setLocations(ifNotNullF(source.getLocations(), locationConverter::convert));
        response.setS3(ifNotNullF(source.getS3(), cloudStorageConverter::convert));
        response.setAdls(ifNotNullF(source.getAdls(), cloudStorageConverter::convert));
        response.setAdlsGen2(ifNotNullF(source.getAdlsGen2(), cloudStorageConverter::convert));
        response.setAdlsGen2(ifNotNullF(source.getAdlsGen2(), cloudStorageConverter::convert));
        return response;
    }
}
