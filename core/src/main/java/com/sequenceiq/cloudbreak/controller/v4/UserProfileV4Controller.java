package com.sequenceiq.cloudbreak.controller.v4;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.UserProfileV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.requests.ShowTerminatedClustersPreferencesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.ShowTerminatedClusterPreferencesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.UserProfileV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.service.stack.ShowTerminatedClustersConfig;
import com.sequenceiq.cloudbreak.service.stack.ShowTerminatedClusterConfigService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;

@Controller
@Transactional(TxType.NEVER)
public class UserProfileV4Controller implements UserProfileV4Endpoint {

    @Inject
    private UserProfileService userProfileService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private ShowTerminatedClusterConfigService showTerminatedClusterConfigService;

    @Override
    public UserProfileV4Response get() {
        UserProfile userProfile = userProfileService.getOrCreateForLoggedInUser();
        return converterUtil.convert(userProfile, UserProfileV4Response.class);
    }

    @Override
    public ShowTerminatedClusterPreferencesV4Response getShowClusterPreferences() {
        return converterUtil.convert(showTerminatedClusterConfigService.getConfig(), ShowTerminatedClusterPreferencesV4Response.class);
    }

    @Override
    public void putTerminatedClustersPreferences(@Valid ShowTerminatedClustersPreferencesV4Request showInstancesPrefsV4Request) {
        ShowTerminatedClustersConfig showTerminatedClustersConfig =
                converterUtil.convert(showInstancesPrefsV4Request, ShowTerminatedClustersConfig.class);
        showTerminatedClusterConfigService.set(showTerminatedClustersConfig);
    }

    @Override
    public void deleteTerminatedClustersPreferences() {
        showTerminatedClusterConfigService.delete();
    }
}
