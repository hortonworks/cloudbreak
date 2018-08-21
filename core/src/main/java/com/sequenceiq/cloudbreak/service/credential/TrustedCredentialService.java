package com.sequenceiq.cloudbreak.service.credential;

import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.organization.OrganizationAwareResourceService;

public interface TrustedCredentialService extends OrganizationAwareResourceService<Credential> {

    void createWithRetry(Credential credential);
}
