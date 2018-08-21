package com.sequenceiq.cloudbreak.service.mpack;

import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.service.organization.LegacyOrganizationAwareResourceService;

public interface LegacyManagementPackService extends LegacyOrganizationAwareResourceService<ManagementPack>, ManagementPackService {
}
