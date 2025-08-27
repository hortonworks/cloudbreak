package com.sequenceiq.environment.api.v1.encryptionprofile.model;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.environment.api.GeneralCollectionV1Response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EncryptionProfileResponses", description = "Wrapper which contains multiple EncryptionProfileResponse")
public class EncryptionProfileResponses extends GeneralCollectionV1Response<EncryptionProfileResponse> {

    public EncryptionProfileResponses(List<EncryptionProfileResponse> responses) {
        super(responses);
    }

    public EncryptionProfileResponses() {
        super(new ArrayList<>());
    }
}
