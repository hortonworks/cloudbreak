package com.sequenceiq.cloudbreak.controller;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.UserV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.users.UserProfileRequest;
import com.sequenceiq.cloudbreak.api.model.users.UserProfileResponse;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class UserV3Controller implements UserV3Endpoint {

    @Inject
    private UserProfileService userProfileService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private UserService userService;

    @Inject
    private WorkspaceService workspaceService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public UserProfileResponse getProfileInWorkspace(Long workspaceId) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        UserProfile userProfile = userProfileService.getOrCreate(user);
        return conversionService.convert(userProfile, UserProfileResponse.class);
    }

    @Override
    public void modifyProfileInWorkspace(Long workspaceId, UserProfileRequest userProfileRequest) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        userProfileService.put(userProfileRequest, user, workspace);
    }
}
