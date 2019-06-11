package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.credential.CredentialCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.credential.CredentialCreateIfNotExistAction;
import com.sequenceiq.it.cloudbreak.action.v4.credential.CredentialDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.credential.CredentialGetAction;
import com.sequenceiq.it.cloudbreak.action.v4.credential.CredentialListAction;
import com.sequenceiq.it.cloudbreak.action.v4.credential.CredentialModifyAction;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;

@Service
public class CredentialTestClient {

    public Action<CredentialTestDto, CloudbreakClient> createV4() {
        return new CredentialCreateAction();
    }

    public Action<CredentialTestDto, CloudbreakClient> deleteV4() {
        return new CredentialDeleteAction();
    }

    public Action<CredentialTestDto, CloudbreakClient> listV4() {
        return new CredentialListAction();
    }

    public Action<CredentialTestDto, CloudbreakClient> createIfNotExistV4() {
        return new CredentialCreateIfNotExistAction();
    }

    public Action<CredentialTestDto, CloudbreakClient> modifyV4() {
        return new CredentialModifyAction();
    }

    public Action<CredentialTestDto, CloudbreakClient> getV4() {
        return new CredentialGetAction();
    }

}
