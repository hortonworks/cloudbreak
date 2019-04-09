package com.sequenceiq.cloudbreak.converter.events;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.CredentialViewV4Response;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.util.GovCloudFlagUtil;

@Component
public class ExtendedCloudCredentialToCredentialViewV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<ExtendedCloudCredential, CredentialViewV4Response> {

    @Override
    public CredentialViewV4Response convert(ExtendedCloudCredential source) {
        CredentialViewV4Response credentialJson = new CredentialViewV4Response();
        credentialJson.setName(source.getName());
        credentialJson.setCloudPlatform(source.getCloudPlatform());
        credentialJson.setGovCloud(isGovCloud(source));
        return credentialJson;
    }

    private Boolean isGovCloud(ExtendedCloudCredential source) {
        Object o = source.getParameters().get(GovCloudFlagUtil.GOV_CLOUD_KEY);
        if (o == null) {
            return true;
        }
        return Boolean.valueOf(o.toString());
    }

}
