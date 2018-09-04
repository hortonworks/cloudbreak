package com.sequenceiq.cloudbreak.service.user;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.organization.User;

@Service
public class UserProfileHandler {

    @Inject
    private UserProfileService userProfileService;

    public void createProfilePreparation(Credential credential, User user) {
        UserProfile userProfile = userProfileService.getOrCreate(credential.getAccount(), credential.getOwner(), user);
        if (userProfile != null && userProfile.getDefaultCredentials().isEmpty()) {
            userProfile.setDefaultCredentials(Sets.newHashSet(credential));
            userProfileService.save(userProfile);
        }
    }

    public void destroyProfileCredentialPreparation(Credential credential) {
        Set<UserProfile> userProfiles = userProfileService.findOneByCredentialId(credential.getId());
        for (UserProfile userProfile : userProfiles) {
            Optional<Credential> foundCredential = Optional.ofNullable(userProfile.getDefaultCredentials()).orElse(Collections.emptySet())
                    .stream()
                    .filter(defaultCredential -> defaultCredential.getId().equals(credential.getId()))
                    .findFirst();
            if (foundCredential.isPresent()) {
                userProfile.getDefaultCredentials().remove(foundCredential.get());
            }
            userProfileService.save(userProfile);
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
