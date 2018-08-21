package com.sequenceiq.cloudbreak.service.credential;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.service.organization.LegacyOrganizationAwareResourceService;

public interface LegacyCredentialService extends LegacyOrganizationAwareResourceService<Credential>, CredentialService {

    Map<String, String> interactiveLogin(Credential credential);

    Set<Credential> listForUsersDefaultOrganization();

    Set<Credential> findAllCredentialByTopology(Topology topology);
}
