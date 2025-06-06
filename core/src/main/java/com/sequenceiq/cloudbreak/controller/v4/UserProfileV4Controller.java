package com.sequenceiq.cloudbreak.controller.v4;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.UserProfileV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.requests.ShowTerminatedClustersPreferencesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.ShowTerminatedClusterPreferencesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.UserProfileV4Response;
import com.sequenceiq.cloudbreak.converter.v4.userprofiles.ShowTerminatedClusterConfigToShowTerminatedClusterPreferencesV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.userprofiles.ShowTerminatedClustersPreferencesV4RequestToShowTerminatedClustersPreferencesConverter;
import com.sequenceiq.cloudbreak.converter.v4.userprofiles.UserProfileToUserProfileV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.service.stack.ShowTerminatedClusterConfigService;
import com.sequenceiq.cloudbreak.service.stack.ShowTerminatedClustersConfig;
import com.sequenceiq.cloudbreak.service.user.UserProfileDecorator;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;

@Controller
@DisableCheckPermissions
@Transactional(TxType.NEVER)
public class UserProfileV4Controller implements UserProfileV4Endpoint {

    @Inject
    private UserProfileService userProfileService;

    @Inject
    private ShowTerminatedClusterConfigService showTerminatedClusterConfigService;

    @Inject
    private UserProfileDecorator userProfileDecorator;

    @Inject
    private ShowTerminatedClusterConfigToShowTerminatedClusterPreferencesV4ResponseConverter showTerminatedClusterConfigConverter;

    @Inject
    private ShowTerminatedClustersPreferencesV4RequestToShowTerminatedClustersPreferencesConverter showTerminatedClustersPreferencesConverter;

    @Inject
    private UserProfileToUserProfileV4ResponseConverter userProfileToUserProfileV4ResponseConverter;

    @Override
    public UserProfileV4Response get() {
        UserProfile userProfile = userProfileService.getOrCreateForLoggedInUser();
        UserProfileV4Response userProfileV4Response = userProfileToUserProfileV4ResponseConverter.convert(userProfile);
        userProfileDecorator.decorate(userProfileV4Response, userProfile.getUser().getUserCrn());
        return userProfileV4Response;
    }

    @Override
    public ShowTerminatedClusterPreferencesV4Response getShowClusterPreferences() {
        return showTerminatedClusterConfigConverter
                .convert(showTerminatedClusterConfigService.getConfig());
    }

    @Override
    public void putTerminatedClustersPreferences(ShowTerminatedClustersPreferencesV4Request showInstancesPrefsV4Request) {
        ShowTerminatedClustersConfig showTerminatedClustersConfig =
                showTerminatedClustersPreferencesConverter
                        .convert(showInstancesPrefsV4Request);
        showTerminatedClusterConfigService.set(showTerminatedClustersConfig);
    }

    @Override
    public void deleteTerminatedClustersPreferences() {
        showTerminatedClusterConfigService.delete();
    }
}
