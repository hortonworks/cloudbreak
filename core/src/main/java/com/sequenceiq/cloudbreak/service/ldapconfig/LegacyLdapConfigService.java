package com.sequenceiq.cloudbreak.service.ldapconfig;

import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.service.organization.LegacyOrganizationAwareResourceService;

public interface LegacyLdapConfigService extends LegacyOrganizationAwareResourceService<LdapConfig>, LdapConfigService {
}
