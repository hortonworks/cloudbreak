package com.sequenceiq.cloudbreak.service.user;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.UserProfileRequest;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.UserProfileRepository;

@Service
@Transactional
public class UserProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileService.class);

    @Inject
    private UserProfileRepository userProfileRepository;

    @Inject
    private CredentialRepository credentialRepository;

    public UserProfile get(String account, String owner) {
        UserProfile userProfile = userProfileRepository.findOneByOwnerAndAccount(account, owner);
        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setAccount(account);
            userProfile.setOwner(owner);
            addUiProperties(userProfile);
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
        UserProfile userProfile = get(user.getAccount(), user.getUserId());
        if (request.getCredentialId() != null) {
            Credential credential = credentialRepository.findByIdInAccount(request.getCredentialId(), userProfile.getAccount());
            if (credential == null) {
                throw new NotFoundException(String.format("Credential '%s' not found in the specified account.", request.getCredentialId()));
            }
            userProfile.setCredential(credential);
        } else if (request.getCredentialName() != null) {
            try {
                Credential credential = credentialRepository.findOneByName(request.getCredentialName(), userProfile.getAccount());
                userProfile.setCredential(credential);
            } catch (Exception ignored) {
                throw new NotFoundException(String.format("Credential '%s' not found in the specified account.", request.getCredentialName()));
            }
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
