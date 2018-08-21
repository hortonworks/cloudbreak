package com.sequenceiq.cloudbreak.service.credential;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.organization.OrganizationAwareResourceService;

public interface CredentialService extends OrganizationAwareResourceService<Credential> {

    Map<String, String> interactiveLogin(Long organizationId, Credential credential);

    Set<Credential> listAvailablesByOrganizationId(Long orgId);

    void archiveCredential(Credential credential);

    Set<Credential> findAllByCloudPlatform(String cloudPlatform);

    void saveAllCredential(Iterable<Credential> credentials);
}
