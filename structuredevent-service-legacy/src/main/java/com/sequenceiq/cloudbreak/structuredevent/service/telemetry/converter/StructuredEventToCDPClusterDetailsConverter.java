package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

@Component
public class StructuredEventToCDPClusterDetailsConverter {

    @Inject
    private StructuredEventToCDPClusterShapeConverter clusterShapeConverter;

    @Inject
    private StructuredEventToCDPImageDetailsConverter imageDetailsConverter;

    @Inject
    private StructuredEventToCDPVersionDetailsConverter versionDetailsConverter;

    public UsageProto.CDPClusterDetails convert(StructuredFlowEvent structuredFlowEvent) {

        UsageProto.CDPClusterDetails.Builder cdpClusterDetails = UsageProto.CDPClusterDetails.newBuilder();

        cdpClusterDetails.setClusterShape(clusterShapeConverter.convert(structuredFlowEvent));
        cdpClusterDetails.setImageDetails(imageDetailsConverter.convert(structuredFlowEvent));
        cdpClusterDetails.setVersionDetails(versionDetailsConverter.convert(structuredFlowEvent));
        if (structuredFlowEvent != null && structuredFlowEvent.getStack() != null) {
            setTags(cdpClusterDetails::setUserTags, structuredFlowEvent.getStack().getStackTags().getUserDefinedTags());
            setTags(cdpClusterDetails::setApplicationTags, structuredFlowEvent.getStack().getStackTags().getApplicationTags());
            String platformVariant = structuredFlowEvent.getStack().getPlatformVariant();
            if (!Strings.isNullOrEmpty(platformVariant)) {
                cdpClusterDetails.setCloudProviderVariant(UsageProto.CDPCloudProviderVariantType
                        .Value.valueOf(platformVariant));
            }
            cdpClusterDetails.setMultiAz(structuredFlowEvent.getStack().isMultiAz());
        }

        return cdpClusterDetails.build();
    }

    public UsageProto.CDPClusterDetails convert(StructuredSyncEvent structuredSyncEvent) {

        UsageProto.CDPClusterDetails.Builder cdpClusterDetails = UsageProto.CDPClusterDetails.newBuilder();

        cdpClusterDetails.setClusterShape(clusterShapeConverter.convert(structuredSyncEvent));
        cdpClusterDetails.setImageDetails(imageDetailsConverter.convert(structuredSyncEvent));
        cdpClusterDetails.setVersionDetails(versionDetailsConverter.convert(structuredSyncEvent));
        if (structuredSyncEvent != null && structuredSyncEvent.getStack() != null) {
            setTags(cdpClusterDetails::setUserTags, structuredSyncEvent.getStack().getStackTags().getUserDefinedTags());
            setTags(cdpClusterDetails::setApplicationTags, structuredSyncEvent.getStack().getStackTags().getApplicationTags());
            String platformVariant = structuredSyncEvent.getStack().getPlatformVariant();
            if (!Strings.isNullOrEmpty(platformVariant)) {
                cdpClusterDetails.setCloudProviderVariant(UsageProto.CDPCloudProviderVariantType
                        .Value.valueOf(platformVariant));
            }
            cdpClusterDetails.setMultiAz(structuredSyncEvent.getStack().isMultiAz());
        }

        return cdpClusterDetails.build();
    }

    private void setTags(final Consumer<String> setter, Map<String, String> tags) {
        if (tags != null && !tags.isEmpty()) {
            setter.accept(JsonUtil.writeValueAsStringSilentSafe(tags));
        }
    }
}
