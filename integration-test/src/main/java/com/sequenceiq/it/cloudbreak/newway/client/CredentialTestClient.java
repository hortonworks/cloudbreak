package com.sequenceiq.it.cloudbreak.newway.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.credential.CredentialCreateAction;

@Service
public class CredentialTestClient {

    public Action<CredentialEntity> post() {
        return new CredentialCreateAction();
    }
}
