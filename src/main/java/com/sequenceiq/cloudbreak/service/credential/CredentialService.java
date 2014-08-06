package com.sequenceiq.cloudbreak.service.credential;

import java.io.File;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.User;

public interface CredentialService {

    Credential save(User user, Credential credential);

    Set<Credential> getAll(User user);

    Credential get(Long id);

    void delete(Long id);

    File getSshPublicKeyFile(User user, Long credentialId);

}
