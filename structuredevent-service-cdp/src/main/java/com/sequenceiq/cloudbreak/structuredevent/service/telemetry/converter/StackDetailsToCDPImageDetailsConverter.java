package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPImageDetails;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.ImageDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;

@Component
public class StackDetailsToCDPImageDetailsConverter {

    public CDPImageDetails convert(StackDetails stackDetails) {
        CDPImageDetails.Builder cdpImageDetails = CDPImageDetails.newBuilder();
        if (stackDetails != null) {
            ImageDetails image = stackDetails.getImage();
            if (image != null) {
                cdpImageDetails.setImageCatalog(defaultIfEmpty(image.getImageCatalogName(), ""));
                cdpImageDetails.setImageId(defaultIfEmpty(image.getImageId(), ""));
                cdpImageDetails.setImageCatalogUrl(defaultIfEmpty(image.getImageCatalogUrl(), ""));
                cdpImageDetails.setOsType(defaultIfEmpty(image.getOsType(), ""));
                cdpImageDetails.setImageName(defaultIfEmpty(image.getImageName(), ""));
            }
        }

        return cdpImageDetails.build();
    }
}
