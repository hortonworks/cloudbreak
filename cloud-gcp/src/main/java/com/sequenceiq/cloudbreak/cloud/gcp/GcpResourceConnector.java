package com.sequenceiq.cloudbreak.cloud.gcp;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingDoesNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceNameService;
import com.sequenceiq.cloudbreak.cloud.template.AbstractResourceConnector;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class GcpResourceConnector extends AbstractResourceConnector {

    @Override
    public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        return new TlsInfo(false);
    }

    @Override
    public String getStackTemplate() throws TemplatingDoesNotSupportedException {
        throw new TemplatingDoesNotSupportedException();
    }

    @Override
    protected List<CloudResource> collectProviderSpecificResources(List<CloudResource> resources, List<CloudInstance> vms) {
        List<CloudResource> result = new ArrayList<>();
        for (CloudInstance instance : vms) {
            String instanceId = instance.getInstanceId();
            String baseName = instanceId.substring(0, instanceId.lastIndexOf(CloudbreakResourceNameService.DELIMITER));
            for (CloudResource resource : resources) {
                if (resource.getType() == ResourceType.GCP_RESERVED_IP && resource.getName().startsWith(baseName)) {
                    result.add(resource);
                }
            }
        }
        return result;
    }
}
