package com.sequenceiq.cloudbreak.service.credential;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Credential;

public interface CredentialService {

    Set<Credential> retrievePrivateCredentials(CbUser user);

    Set<Credential> retrieveAccountCredentials(CbUser user);

    Credential get(Long id);

    Credential create(CbUser user, Credential credential);

    void delete(Long id);

}
