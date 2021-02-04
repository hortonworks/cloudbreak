package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.ImageDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

@Component
public class StructuredFlowEventToVersionDetailsConverter {

    public UsageProto.CDPVersionDetails convert(StructuredFlowEvent structuredFlowEvent) {

        UsageProto.CDPVersionDetails.Builder versionDetails = UsageProto.CDPVersionDetails.newBuilder();

        if (structuredFlowEvent != null) {
            StackDetails stackDetails = structuredFlowEvent.getStack();
            if (stackDetails != null) {
                ImageDetails image = stackDetails.getImage();
                if (image != null) {
                    Map<String, String> packageVersions = image.getPackageVersions();
                    if (packageVersions != null) {
                        versionDetails.setCmVersion(packageVersions.getOrDefault("cm", ""));
                        versionDetails.setCdpdVersion(packageVersions.getOrDefault("stack", ""));
                        versionDetails.setCrVersion(packageVersions.getOrDefault("stack", ""));
                        versionDetails.setSaltVersion(packageVersions.getOrDefault("salt", ""));
                    }
                }
            }
        }
        return versionDetails.build();

    }

}
