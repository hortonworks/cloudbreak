package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.ImageDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

@Component
public class StructuredFlowEventToImageDetailsConverter {

    public UsageProto.CDPImageDetails convert(StructuredFlowEvent structuredFlowEvent) {

        UsageProto.CDPImageDetails.Builder cdpImageDetails = UsageProto.CDPImageDetails.newBuilder();

        if (structuredFlowEvent != null) {
            StackDetails stackDetails = structuredFlowEvent.getStack();
            if (stackDetails != null) {
                ImageDetails image = stackDetails.getImage();
                if (image != null) {
                    cdpImageDetails.setImageCatalog(image.getImageCatalogName());
                    cdpImageDetails.setImageId(image.getImageId());
                }
            }
        }

        return cdpImageDetails.build();

    }
}
