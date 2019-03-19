package com.sequenceiq.it.cloudbreak.newway.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.v4.credential.CredentialCreateAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.credential.CredentialCreateIfNotExistAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.credential.CredentialDeleteAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.credential.CredentialGetAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.credential.CredentialListAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.credential.CredentialModifyAction;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;

@Service
public class CredentialTestClient {

    public Action<CredentialTestDto> createV4() {
        return new CredentialCreateAction();
    }

    public Action<CredentialTestDto> deleteV4() {
        return new CredentialDeleteAction();
    }

    public Action<CredentialTestDto> listV4() {
        return new CredentialListAction();
    }

    public Action<CredentialTestDto> createIfNotExistV4() {
        return new CredentialCreateIfNotExistAction();
    }

    public Action<CredentialTestDto> modifyV4() {
        return new CredentialModifyAction();
    }

    public Action<CredentialTestDto> getV4() {
        return new CredentialGetAction();
    }

}
