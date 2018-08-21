package com.sequenceiq.cloudbreak.service.rdsconfig;

import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.organization.LegacyOrganizationAwareResourceService;

public interface LegacyRdsConfigService extends LegacyOrganizationAwareResourceService<RDSConfig>, RdsConfigService {
}
