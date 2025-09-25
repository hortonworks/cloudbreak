package com.sequenceiq.environment.authorization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.service.list.AbstractAuthorizationFiltering;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponses;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.encryptionprofile.service.EncryptionProfileService;
import com.sequenceiq.environment.encryptionprofile.v1.converter.EncryptionProfileToEncryptionProfileResponseConverter;

@Component
public class EncryptionProfileFiltering extends AbstractAuthorizationFiltering<EncryptionProfileResponses> {

    private final EncryptionProfileService encryptionProfileService;

    private final EncryptionProfileToEncryptionProfileResponseConverter encryptionProfileResponseConverter;

    public EncryptionProfileFiltering(EncryptionProfileService encryptionProfileService,
            EncryptionProfileToEncryptionProfileResponseConverter encryptionProfileResponseConverter) {
        this.encryptionProfileService = encryptionProfileService;
        this.encryptionProfileResponseConverter = encryptionProfileResponseConverter;
    }

    @Override
    protected List<ResourceWithId> getAllResources(Map<String, Object> args) {
        return encryptionProfileService.getEncryptionProfilesAsAuthorizationResources();
    }

    @Override
    protected EncryptionProfileResponses filterByIds(List<Long> authorizedResourceIds, Map<String, Object> args) {
        List<EncryptionProfile> encryptionProfiles = new ArrayList<>(encryptionProfileService.findAllById(authorizedResourceIds));
        encryptionProfiles.addAll(encryptionProfileService.getAllDefaultEncryptionProfiles());

        return getEncryptionProfileResponses(encryptionProfiles);
    }

    @Override
    protected EncryptionProfileResponses getAll(Map<String, Object> args) {
        List<EncryptionProfile> encryptionProfiles = new ArrayList<>(encryptionProfileService.listAll());
        encryptionProfiles.addAll(encryptionProfileService.getAllDefaultEncryptionProfiles());

        return getEncryptionProfileResponses(encryptionProfiles);
    }

    private EncryptionProfileResponses getEncryptionProfileResponses(List<EncryptionProfile> encryptionProfiles) {
        List<EncryptionProfileResponse> encryptionProfileResponseSet =
                encryptionProfiles
                        .stream()
                        .map(encryptionProfileResponseConverter::convert)
                        .collect(Collectors.toList());

        return new EncryptionProfileResponses(encryptionProfileResponseSet);
    }
}
