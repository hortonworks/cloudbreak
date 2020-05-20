package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient.INTERNAL_ACTOR_CRN;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsResponse;
import com.google.common.collect.Maps;
import com.google.protobuf.Descriptors;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;

@Component
public class UmsEventGenerationIdsProvider {
    @Inject
    private GrpcUmsClient grpcUmsClient;

    public UmsEventGenerationIds getEventGenerationIds(String accountId, Optional<String> requestId) {
        GetEventGenerationIdsResponse response = grpcUmsClient.getEventGenerationIds(INTERNAL_ACTOR_CRN, accountId, requestId);

        UmsEventGenerationIds umsEventGenerationIds = new UmsEventGenerationIds();
        Map<String, String> eventGenerationIdsMap = Maps.newHashMap();
        for (Descriptors.FieldDescriptor fd : response.getDescriptorForType().getFields()) {
            if (Descriptors.FieldDescriptor.Type.STRING.equals(fd.getType()) && response.hasField(fd)) {
                eventGenerationIdsMap.put(fd.getName(), (String) response.getField(fd));
            }
        }
        umsEventGenerationIds.setEventGenerationIds(eventGenerationIdsMap);

        return umsEventGenerationIds;
    }
}