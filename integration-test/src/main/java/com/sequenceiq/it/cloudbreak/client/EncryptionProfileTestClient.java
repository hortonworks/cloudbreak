package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.encryptionprofile.EncryptionProfileCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.encryptionprofile.EncryptionProfileDeleteAction;
import com.sequenceiq.it.cloudbreak.dto.environment.EncryptionProfileTestDto;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

@Service
public class EncryptionProfileTestClient {

    public Action<EncryptionProfileTestDto, EnvironmentClient> create() {
        return new EncryptionProfileCreateAction();
    }

    public Action<EncryptionProfileTestDto, EnvironmentClient> delete() {
        return new EncryptionProfileDeleteAction();
    }
}
