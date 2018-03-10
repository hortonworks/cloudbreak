package com.sequenceiq.cloudbreak.cloud.openstack.common;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient;

@Component
public class OpenStackStackValidator implements Validator {

    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private OpenStackUtils utils;

    @Override
    public void validate(AuthenticatedContext ac, CloudStack cloudStack) {
        OSClient<?> client = openStackClient.createOSClient(ac);
        String stackName = utils.getStackName(ac);
        if (client.heat().stacks().getStackByName(stackName) != null) {
            throw new CloudConnectorException(String.format("Stack is already exists with the given name: %s", stackName));
        }
    }
}
