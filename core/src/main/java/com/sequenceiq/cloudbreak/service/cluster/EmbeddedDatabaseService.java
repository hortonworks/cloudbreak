package com.sequenceiq.cloudbreak.service.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Component
public class EmbeddedDatabaseService {
    @Inject
    private EntitlementService entitlementService;

    public EmbeddedDatabaseInfo getEmbeddedDatabaseInfo(String actorCrn, String accountId, Stack stack) {
        if (entitlementService.embeddedDatabaseOnAttachedDiskEnabled(actorCrn, accountId)) {
            Template template = stack.getPrimaryGatewayInstance().getInstanceGroup().getTemplate();
            int volumeCount = template == null ? 0 : template.getVolumeTemplates().stream()
                    .mapToInt(volume -> volume.getVolumeCount()).sum();
            return new EmbeddedDatabaseInfo(volumeCount > 0, volumeCount);
        } else {
            return new EmbeddedDatabaseInfo(false, 0);
        }
    }
}
