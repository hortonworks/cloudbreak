package com.sequenceiq.cloudbreak.converter.v4.userprofiles;

import java.time.Duration;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.DurationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.ShowTerminatedClusterPreferencesV4Response;
import com.sequenceiq.cloudbreak.service.stack.ShowTerminatedClustersConfig;

@Component
public class ShowTerminatedClusterConfigToShowTerminatedClusterPreferencesV4ResponseConverter {

    @Inject
    private DurationToDurationV4ResponseConverter durationToDurationV4ResponseConverter;

    public ShowTerminatedClusterPreferencesV4Response convert(ShowTerminatedClustersConfig source) {
        ShowTerminatedClusterPreferencesV4Response showTerminatedClusterPreferencesV4Response = new ShowTerminatedClusterPreferencesV4Response();
        showTerminatedClusterPreferencesV4Response.setSource(source.getSource().toString());
        showTerminatedClusterPreferencesV4Response.setActive(source.isActive());
        showTerminatedClusterPreferencesV4Response.setTimeout(getTimeout(source.getTimeout()));
        return showTerminatedClusterPreferencesV4Response;
    }

    private DurationV4Response getTimeout(Duration duration) {
        if (duration == null) {
            return null;
        }

        return durationToDurationV4ResponseConverter.convert(duration);
    }
}
