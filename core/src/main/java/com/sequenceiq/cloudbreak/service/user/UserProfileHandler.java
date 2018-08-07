package com.sequenceiq.cloudbreak.service.user;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.UserProfile;

@Service
public class UserProfileHandler {

    @Inject
    private UserProfileService userProfileService;

    public void createProfilePreparation(Credential credential) {
        UserProfile userProfile = userProfileService.getOrCreate(credential.getAccount(), credential.getOwner());
        if (userProfile != null && userProfile.getCredential() == null) {
            userProfile.setCredential(credential);
            userProfileService.save(userProfile);
        }
    }

    public void destroyProfileCredentialPreparation(Credential credential) {
        Set<UserProfile> userProfiles = userProfileService.findOneByCredentialId(credential.getId());
        for (UserProfile userProfile : userProfiles) {
            userProfile.setCredential(null);
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
