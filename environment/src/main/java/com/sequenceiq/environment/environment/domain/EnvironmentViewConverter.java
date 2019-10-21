package com.sequenceiq.environment.environment.domain;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.credential.v1.converter.CredentialViewConverter;

@Component
public class EnvironmentViewConverter {

    private final CredentialViewConverter credentialViewConverter;

    public EnvironmentViewConverter(CredentialViewConverter credentialViewConverter) {
        this.credentialViewConverter = credentialViewConverter;
    }

    public EnvironmentView convert(Environment environment) {
        EnvironmentView environmentView = new EnvironmentView();
        environmentView.setAccountId(environment.getAccountId());
        environmentView.setArchived(environment.isArchived());
        environmentView.setCloudPlatform(environment.getCloudPlatform());
        environmentView.setCredentialView(credentialViewConverter.convert(environment.getCredential()));
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
