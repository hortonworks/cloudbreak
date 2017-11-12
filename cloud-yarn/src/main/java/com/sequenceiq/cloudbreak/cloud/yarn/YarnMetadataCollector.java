package com.sequenceiq.cloudbreak.cloud.yarn;

import java.net.MalformedURLException;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.yarn.auth.YarnClientUtil;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.orchestrator.yarn.api.YarnResourceConstants;
import com.sequenceiq.cloudbreak.orchestrator.yarn.client.YarnClient;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.Container;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.request.ApplicationDetailRequest;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.response.ApplicationDetailResponse;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.response.ResponseContext;

@Service
public class YarnMetadataCollector implements MetadataCollector {
    @Inject
    private YarnClientUtil yarnClientUtil;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        YarnClient yarnClient = yarnClientUtil.createYarnClient(authenticatedContext);
        CloudResource yarnApplication = getYarnApplcationResource(resources);
        ApplicationDetailRequest applicationDetailRequest = new ApplicationDetailRequest();
        try {
            ResponseContext responseContext = yarnClient.getApplicationDetail(applicationDetailRequest);
            if (responseContext.getStatusCode() == YarnResourceConstants.HTTP_SUCCESS) {
                ApplicationDetailResponse applicationDetailResponse = (ApplicationDetailResponse) responseContext.getResponseObject();
                for (Container container: applicationDetailResponse.getContainers()) {
                    container.getState();
                    // TODO find out metadata collection, some thoughts:
                    // TODO pairing containers to yarn components by componentName
                    // TODO grouping vms by instancegroup
                    // TODO identify containers based on instanceids (maybe container.uniqueid can be used)
                    // TODO collecting private and public ips and vm status from the response
                }
            } else {
                // TODO error handling
                throw new CloudConnectorException("ERROR!!!");
            }
        } catch (MalformedURLException ex) {
            throw new CloudConnectorException("Failed to get yarn application details", ex);
        }
        return null;
    }

    public CloudResource getYarnApplcationResource(List<CloudResource> resourceList) {
        for (CloudResource resource : resourceList) {
            if (resource.getType() == ResourceType.YARN_APPLICATION) {
                return resource;
            }
        }
        throw new CloudConnectorException(String.format("No resource found: %s", ResourceType.YARN_APPLICATION));
    }
}
