package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                        String cmVersion = String.format("%s-%s",
                                packageVersions.getOrDefault("cm", ""), packageVersions.getOrDefault("cm-build-number", ""));
                        String cdpdVersion = String.format("%s-%s",
                                packageVersions.getOrDefault("stack", ""), packageVersions.getOrDefault("cdh-build-number", ""));

                        versionDetails.setCmVersion(cmVersion);
                        versionDetails.setCdpdVersion(cdpdVersion);
                        versionDetails.setCrVersion(packageVersions.getOrDefault("stack", ""));
                        versionDetails.setSaltVersion(packageVersions.getOrDefault("salt", ""));
                        versionDetails.setOsPatchLevel(packageVersions.getOrDefault("date", ""));

                        List<String> formattedPackageVersions = packageVersions.entrySet().stream()
                                .map(x -> String.format("%s=%s", x.getKey(), x.getValue()))
                                .sorted()
                                .collect(Collectors.toList());

                        versionDetails.setAll(String.join(", ", formattedPackageVersions));
                    }
                }
            }
        }
        return versionDetails.build();

    }

}
