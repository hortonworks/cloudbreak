package com.sequenceiq.cloudbreak.cloud.azure.validator;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Component
public class AzureStorageValidator implements Validator {

    @Inject
    private AzureUtils azureUtils;

    @Override
    public void validate(AuthenticatedContext ac, CloudStack cloudStack) {
        azureUtils.validateStorageType(cloudStack);
    }
}
