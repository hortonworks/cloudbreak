package com.sequenceiq.cloudbreak.service.proxy;

import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.service.organization.LegacyOrganizationAwareResourceService;

public interface LegacyProxyConfigService extends LegacyOrganizationAwareResourceService<ProxyConfig>, ProxyConfigService {
}
