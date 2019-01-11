package com.sequenceiq.cloudbreak.controller.v4;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.UserProfileV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.requests.UserProfileV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.UserEvictV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.UserProfileV4Response;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.CachedUserService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class UserProfileV4Controller implements UserProfileV4Endpoint {

    @Inject
    private UserProfileService userProfileService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private UserService userService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private CachedUserService cachedUserService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public UserProfileV4Response get(Long workspaceId) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        UserProfile userProfile = userProfileService.getOrCreate(user);
        return conversionService.convert(userProfile, UserProfileV4Response.class);
    }

    @Override
    public UserProfileV4Response modify(Long workspaceId, UserProfileV4Request userProfileV4Request) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        UserProfile userProfile = userProfileService.put(userProfileV4Request, user, workspace);
        return conversionService.convert(userProfile, UserProfileV4Response.class);
    }

    @Override
    public UserEvictV4Response evictCurrentUserDetails() {
        CloudbreakUser user = restRequestThreadLocalService.getCloudbreakUser();
        cachedUserService.evictByIdentityUser(user);
        return new UserEvictV4Response(user.getUsername());
    }
}
