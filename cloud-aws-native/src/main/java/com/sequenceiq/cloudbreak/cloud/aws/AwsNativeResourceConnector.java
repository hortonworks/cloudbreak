package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.template.AbstractResourceConnector;

@Component
public class AwsNativeResourceConnector extends AbstractResourceConnector {

    @Override
    public List<CloudResourceStatus> launchLoadBalancers(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier)
            throws Exception {
        return null;
    }

    @Override
    public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        return null;
    }

    @Override
    public String getStackTemplate() throws TemplatingNotSupportedException {
        throw new TemplatingNotSupportedException();
    }

    @Override
    public String getDBStackTemplate() throws TemplatingNotSupportedException {
        return "";
    }

    @Override
    protected List<CloudResource> collectProviderSpecificResources(List<CloudResource> resources, List<CloudInstance> vms) {
        return null;
    }
}
