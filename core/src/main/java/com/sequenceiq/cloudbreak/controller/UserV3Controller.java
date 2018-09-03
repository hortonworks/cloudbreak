package com.sequenceiq.cloudbreak.controller;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v3.UserV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.users.UserProfileRequest;
import com.sequenceiq.cloudbreak.api.model.users.UserProfileResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
@Transactional(Transactional.TxType.NEVER)
public class UserV3Controller implements UserV3Endpoint {

    @Inject
    private UserProfileService userProfileService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private UserService userService;

    @Inject
    private OrganizationService organizationService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public UserProfileResponse getProfileInOrganization(Long organizationId) {
        IdentityUser identityUser = restRequestThreadLocalService.getIdentityUser();
        User user = userService.getOrCreate(identityUser);
        UserProfile userProfile = userProfileService.getOrCreate(identityUser.getAccount(), identityUser.getUserId(), identityUser.getUsername(), user);
        return conversionService.convert(userProfile, UserProfileResponse.class);
    }

    @Override
    public void modifyProfileInOrganization(Long organizationId, UserProfileRequest userProfileRequest) {
        IdentityUser identityUser = restRequestThreadLocalService.getIdentityUser();
        User user = userService.getOrCreate(identityUser);
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        userProfileService.put(userProfileRequest, identityUser, user, organization);
    }
}
