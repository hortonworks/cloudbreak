package com.sequenceiq.cloudbreak.converter.v4.imagecatalog;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseStackDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.common.model.Architecture;

@Component
public class ImageToImageV4ResponseConverter {

    public ImageV4Response convert(Image source) {
        ImageV4Response result = new ImageV4Response();
        result.setDate(source.getDate());
        result.setCreated(source.getCreated());
        result.setPublished(source.getPublished());
        result.setDescription(source.getDescription());
        result.setOs(source.getOs());
        result.setOsType(source.getOsType());
        result.setArchitecture(Architecture.fromStringWithFallback(source.getArchitecture()).getName());
        result.setUuid(source.getUuid());
        result.setVersion(source.getVersion());
        result.setDefaultImage(source.isDefaultImage());
        result.setPackageVersions(source.getPackageVersions());
        result.setRepository(source.getRepo());
        result.setImageSetsByProvider(source.getImageSetsByProvider());
        result.setCmBuildNumber(source.getCmBuildNumber());
        result.setPreWarmCsd(source.getPreWarmCsd());
        result.setPreWarmParcels(source.getPreWarmParcels());
        result.setAdvertised(source.isAdvertised());

        ImageStackDetails stackDetails = source.getStackDetails();
        if (stackDetails != null) {
            BaseStackDetailsV4Response stackDetailsResponse = new BaseStackDetailsV4Response();
            stackDetailsResponse.setStackBuildNumber(stackDetails.getStackBuildNumber());
            stackDetailsResponse.setVersion(stackDetails.getVersion());
            result.setStackDetails(stackDetailsResponse);
        }

        result.setBaseParcelUrl(source.getBaseParcelUrl());
        result.setSourceImageId(source.getSourceImageId());

        return result;
    }
}
