package com.sequenceiq.environment.encryptionprofile.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileRequest;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;

@Component
public class EncryptionProfileRequestToEncryptionProfileConverter {

    public EncryptionProfile convert(EncryptionProfileRequest source) {
        EncryptionProfile encryptionProfile = new EncryptionProfile();
        encryptionProfile.setName(source.getName());
        encryptionProfile.setTlsVersions(source.getTlsVersions());
        encryptionProfile.setCipherSuites(source.getCipherSuites());
        encryptionProfile.setDescription(source.getDescription());
        encryptionProfile.setResourceStatus(ResourceStatus.USER_MANAGED);
        return encryptionProfile;
    }
}
