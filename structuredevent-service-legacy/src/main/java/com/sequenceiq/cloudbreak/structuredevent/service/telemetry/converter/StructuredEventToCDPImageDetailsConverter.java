package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.ImageDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

@Component
public class StructuredEventToCDPImageDetailsConverter {

    public UsageProto.CDPImageDetails convert(StructuredFlowEvent structuredFlowEvent) {

        UsageProto.CDPImageDetails.Builder cdpImageDetails = UsageProto.CDPImageDetails.newBuilder();

        if (structuredFlowEvent != null) {
            cdpImageDetails = convert(structuredFlowEvent.getStack());
        }

        return cdpImageDetails.build();
    }

    public UsageProto.CDPImageDetails convert(StructuredSyncEvent structuredSyncEvent) {

        UsageProto.CDPImageDetails.Builder cdpImageDetails = UsageProto.CDPImageDetails.newBuilder();

        if (structuredSyncEvent != null) {
            cdpImageDetails = convert(structuredSyncEvent.getStack());
        }

        return cdpImageDetails.build();
    }

    private UsageProto.CDPImageDetails.Builder convert(StackDetails stackDetails) {

        UsageProto.CDPImageDetails.Builder cdpImageDetails = UsageProto.CDPImageDetails.newBuilder();

        if (stackDetails != null) {
            ImageDetails image = stackDetails.getImage();
            if (image != null) {
                cdpImageDetails.setImageCatalog(defaultIfEmpty(image.getImageCatalogName(), ""));
                cdpImageDetails.setImageId(defaultIfEmpty(image.getImageId(), ""));
                cdpImageDetails.setImageCatalogUrl(defaultIfEmpty(image.getImageCatalogUrl(), ""));
                cdpImageDetails.setOsType(defaultIfEmpty(image.getOsType(), ""));
            }
        }

        return cdpImageDetails;

    }
}
