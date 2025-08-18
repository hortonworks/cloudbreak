package com.sequenceiq.environment.encryptionprofile.v1.converter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.tls.DefaultEncryptionProfileProvider;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileRequest;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;

@Component
public class EncryptionProfileRequestToEncryptionProfileConverter {

    @Autowired
    private DefaultEncryptionProfileProvider defaultEncryptionProfileProvider;

    public EncryptionProfile convert(EncryptionProfileRequest source) {
        EncryptionProfile encryptionProfile = new EncryptionProfile();
        encryptionProfile.setName(source.getName());
        encryptionProfile.setTlsVersions(source.getTlsVersions());
        // TODO fix cipher ordering to convert set to list in api pojos
        List<String> cipherSuitesToIana = defaultEncryptionProfileProvider.convertCipherSuitesToIana(new ArrayList<>(source.getCipherSuites()));
        encryptionProfile.setCipherSuites(new HashSet<>(cipherSuitesToIana));
        encryptionProfile.setDescription(source.getDescription());
        encryptionProfile.setResourceStatus(ResourceStatus.USER_MANAGED);
        return encryptionProfile;
    }
}
