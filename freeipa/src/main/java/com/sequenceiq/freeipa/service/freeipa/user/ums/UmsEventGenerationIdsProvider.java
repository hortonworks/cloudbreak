package com.sequenceiq.freeipa.service.freeipa.user.ums;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.EventGenerationIds;
import com.google.common.collect.Maps;
import com.google.protobuf.Descriptors;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;

@Component
public class UmsEventGenerationIdsProvider {
    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public UmsEventGenerationIds getEventGenerationIds(String accountId) {
        EventGenerationIds eventGenerationIds =
                grpcUmsClient.getEventGenerationIds(accountId, regionAwareInternalCrnGeneratorFactory)
                .getEventGenerationIds();

        UmsEventGenerationIds umsEventGenerationIds = new UmsEventGenerationIds();
        Map<String, String> eventGenerationIdsMap = Maps.newHashMap();
        for (Descriptors.FieldDescriptor fd : eventGenerationIds.getDescriptorForType().getFields()) {
            if (Descriptors.FieldDescriptor.Type.STRING.equals(fd.getType()) && eventGenerationIds.hasField(fd)) {
                eventGenerationIdsMap.put(fd.getName(), (String) eventGenerationIds.getField(fd));
            }
        }
        umsEventGenerationIds.setEventGenerationIds(eventGenerationIdsMap);

        return umsEventGenerationIds;
    }
}