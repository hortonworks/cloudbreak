package com.sequenceiq.cloudbreak.service.user;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.workspace.User;

@Service
public class UserProfileHandler {

    @Inject
    private UserProfileService userProfileService;

    public void createProfilePreparation(Credential credential, User user) {
        UserProfile userProfile = userProfileService.getOrCreate(user);
        if (userProfile != null && userProfile.getDefaultCredentials().isEmpty()) {
            userProfile.setDefaultCredentials(Sets.newHashSet(credential));
            userProfileService.save(userProfile);
        }
    }

    public void destroyProfileCredentialPreparation(Credential credential) {
        Set<UserProfile> userProfiles = userProfileService.findOneByCredentialId(credential.getId());
        for (UserProfile userProfile : userProfiles) {
            if (userProfile.getDefaultCredentials() != null && !userProfile.getDefaultCredentials().isEmpty()) {
                Set<Credential> foundCredentials = userProfile.getDefaultCredentials()
                        .stream()
                        .filter(defaultCredential -> defaultCredential.getId().equals(credential.getId()))
                        .collect(Collectors.toSet());
                userProfile.getDefaultCredentials().removeAll(foundCredentials);
                userProfileService.save(userProfile);
            }
        }
    }

    public void destroyProfileImageCatalogPreparation(ImageCatalog imageCatalog) {
        Set<UserProfile> userProfiles = userProfileService.findByImageCatalogId(imageCatalog.getId());
        for (UserProfile userProfile : userProfiles) {
            userProfile.setImageCatalog(null);
            userProfileService.save(userProfile);
        }
    }
}
