package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.credential.CredentialCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.credential.CredentialCreateIfNotExistAction;
import com.sequenceiq.it.cloudbreak.action.v4.credential.CredentialDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.credential.CredentialGetAction;
import com.sequenceiq.it.cloudbreak.action.v4.credential.CredentialListAction;
import com.sequenceiq.it.cloudbreak.action.v4.credential.CredentialModifyAction;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

@Service
public class CredentialTestClient {

    public Action<CredentialTestDto, EnvironmentClient> create() {
        return new CredentialCreateAction();
    }

    public Action<CredentialTestDto, EnvironmentClient> delete() {
        return new CredentialDeleteAction();
    }

    public Action<CredentialTestDto, EnvironmentClient> list() {
        return new CredentialListAction();
    }

    public Action<CredentialTestDto, EnvironmentClient> createIfNotExist() {
        return new CredentialCreateIfNotExistAction();
    }

    public Action<CredentialTestDto, EnvironmentClient> modify() {
        return new CredentialModifyAction();
    }

    public Action<CredentialTestDto, EnvironmentClient> get() {
        return new CredentialGetAction();
    }

}
