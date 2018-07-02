package com.sequenceiq.cloudbreak.service.user;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.UserProfileRequest;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.repository.UserProfileRepository;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;

@Service
public class UserProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileService.class);

    @Inject
    private UserProfileRepository userProfileRepository;

    @Inject
    private CredentialService credentialService;

    public UserProfile getOrCreate(String account, String owner) {
        return getOrCreate(account, owner, null);
    }

    public UserProfile getOrCreate(String account, String owner, String userName) {
        UserProfile userProfile = userProfileRepository.findOneByOwnerAndAccount(account, owner);
        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setAccount(account);
            userProfile.setOwner(owner);
            userProfile.setUserName(userName);
            addUiProperties(userProfile);
            userProfile = userProfileRepository.save(userProfile);
        } else if (userProfile.getUserName() == null && userName != null) {
            userProfile.setUserName(userName);
            userProfile = userProfileRepository.save(userProfile);
        }
        return userProfile;
    }

    public UserProfile save(UserProfile userProfile) {
        return userProfileRepository.save(userProfile);
    }

    public Set<UserProfile> findOneByCredentialId(Long credentialId) {
        return userProfileRepository.findOneByCredentialId(credentialId);
    }

    private void addUiProperties(UserProfile userProfile) {
        try {
            userProfile.setUiProperties(new Json(new HashMap<>()));
        } catch (JsonProcessingException ignored) {
            userProfile.setUiProperties(null);
        }
    }

    public void put(UserProfileRequest request, IdentityUser user) {
        UserProfile userProfile = getOrCreate(user.getAccount(), user.getUserId(), user.getUsername());
        if (request.getCredentialId() != null) {
            Credential credential = credentialService.get(request.getCredentialId(), userProfile.getAccount());
            userProfile.setCredential(credential);
        } else if (request.getCredentialName() != null) {
            Credential credential = credentialService.get(request.getCredentialName(), userProfile.getAccount());
            userProfile.setCredential(credential);
        }
        for (Entry<String, Object> uiStringObjectEntry : request.getUiProperties().entrySet()) {
            Map<String, Object> map = userProfile.getUiProperties().getMap();
            if (map == null || map.isEmpty()) {
                map = new HashMap<>();
            }
            map.put(uiStringObjectEntry.getKey(), uiStringObjectEntry.getValue());
            try {
                userProfile.setUiProperties(new Json(map));
            } catch (JsonProcessingException ignored) {
                throw new BadRequestException("The modification of the ui properties was unsuccesfull.");
            }
        }
        userProfileRepository.save(userProfile);
    }
}
