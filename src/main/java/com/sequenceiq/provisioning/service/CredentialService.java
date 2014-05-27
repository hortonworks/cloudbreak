package com.sequenceiq.provisioning.service;

import java.util.Set;

import com.sequenceiq.provisioning.controller.json.CredentialJson;
import com.sequenceiq.provisioning.domain.User;

public interface CredentialService {

    void save(User user, CredentialJson credentialRequest);

    Set<CredentialJson> getAll(User user);

    CredentialJson get(Long id);

    void delete(Long id);

}
