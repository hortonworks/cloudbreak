package com.sequenceiq.cloudbreak.service.blueprint;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.organization.LegacyOrganizationAwareResourceService;

public interface LegacyBlueprintService extends LegacyOrganizationAwareResourceService<Blueprint>, BlueprintService {
}
