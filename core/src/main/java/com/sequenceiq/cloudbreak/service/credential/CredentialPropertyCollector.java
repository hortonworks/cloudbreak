package com.sequenceiq.cloudbreak.service.credential;

import java.security.InvalidParameterException;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;

@Component
public class CredentialPropertyCollector {

    @Inject
    private CredentialPropertyPropagator credentialPropertyPropagator;

    public Map<String, Object> propertyMap(CredentialV4Request cred) {
        Mappable params = credentialPropertyPropagator.propagateCredentialProperty(cred)
                .orElseThrow(() -> new InvalidParameterException("Unable to propagate credential property"));
        Optional<Map<String, Object>> propertiesAsMap = Optional.empty();
        if (params.getCloudPlatform().equals(CloudPlatform.AWS)) {
            propertiesAsMap = Optional.of(cred.getAws().asMap());
        } else if (params.getCloudPlatform().equals(CloudPlatform.AZURE)) {
            propertiesAsMap = Optional.of(cred.getAzure().asMap());
        } else if (params.getCloudPlatform().equals(CloudPlatform.CUMULUS_YARN)) {
            propertiesAsMap = Optional.of(cred.getCumulus().asMap());
        } else if (params.getCloudPlatform().equals(CloudPlatform.GCP)) {
            propertiesAsMap = Optional.of(cred.getGcp().asMap());
        } else if (params.getCloudPlatform().equals(CloudPlatform.OPENSTACK)) {
            propertiesAsMap = Optional.of(cred.getOpenstack().asMap());
        } else if (params.getCloudPlatform().equals(CloudPlatform.YARN)) {
            propertiesAsMap = Optional.of(cred.getYarn().asMap());
        } else if (params.getCloudPlatform().equals(CloudPlatform.MOCK)) {
            propertiesAsMap = Optional.of(cred.getMock().asMap());
        }
        return propertiesAsMap.orElseThrow(() -> new InvalidParameterException("Unable to collect credential properties"));
    }

}
