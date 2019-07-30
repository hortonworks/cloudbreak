package com.sequenceiq.environment.environment.v1;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;

@Component
public class EnvironmentViewConverter {

    public EnvironmentView convert(Environment environment) {
        EnvironmentView environmentView = new EnvironmentView();
        environmentView.setAccountId(environment.getAccountId());
        environmentView.setArchived(environment.isArchived());
        environmentView.setCloudPlatform(environment.getCloudPlatform());
        environmentView.setCredential(environment.getCredential());
        environmentView.setDescription(environment.getDescription());
        environmentView.setId(environment.getId());
        environmentView.setLatitude(environment.getLatitude());
        environmentView.setLocation(environment.getLocation());
        environmentView.setLocationDisplayName(environment.getLocationDisplayName());
        environmentView.setLongitude(environment.getLongitude());
        environmentView.setName(environment.getName());
        environmentView.setNetwork(environment.getNetwork());
        environmentView.setRegions(environment.getRegions());
        environmentView.setResourceCrn(environment.getResourceCrn());
        environmentView.setStatus(environment.getStatus());
        environmentView.setTelemetry(environment.getTelemetry());
        return environmentView;
    }
}
