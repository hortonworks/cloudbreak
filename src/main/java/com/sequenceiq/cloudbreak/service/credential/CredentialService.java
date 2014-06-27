package com.sequenceiq.cloudbreak.service.credential;

import java.util.Set;

import com.sequenceiq.cloudbreak.controller.json.CredentialJson;
import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.domain.User;

public interface CredentialService {

    IdJson save(User user, CredentialJson credentialRequest);

    Set<CredentialJson> getAll(User user);

    CredentialJson get(Long id);

    void delete(Long id);

}
