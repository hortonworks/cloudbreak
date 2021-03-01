package com.sequenceiq.cloudbreak.cloud.openstack.nativ;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.OS4JException;
import org.openstack4j.api.exceptions.StatusCode;
import org.openstack4j.model.common.ActionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.OpenStackVariant;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

public abstract class AbstractOpenStackResourceBuilder implements CloudPlatformAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOpenStackResourceBuilder.class);

    @Inject
    private OpenStackClient openStackClient;

    protected OSClient<?> createOSClient(AuthenticatedContext auth) {
        return openStackClient.createOSClient(auth);
    }

    protected CloudResource createNamedResource(ResourceType resourceType, String group, String name) {
        return new Builder()
                .name(name)
                .group(group)
                .type(resourceType)
                .status(CommonStatus.REQUESTED)
                .build();
    }

    protected CloudResource createPersistedResource(CloudResource namedResource, String group, String reference) {
        return createPersistedResource(namedResource, group, reference, Maps.newHashMap());
    }

    protected CloudResource createPersistedResource(CloudResource namedResource, String reference) {
        return createPersistedResource(namedResource, null, reference, Maps.newHashMap());
    }

    protected CloudResource createPersistedResource(CloudResource namedResource, String group, String reference, Map<String, Object> params) {
        return new Builder()
                .cloudResource(namedResource)
                .reference(reference)
                .status(CommonStatus.CREATED)
                .group(group)
                .params(params)
                .build();
    }

    protected List<CloudResourceStatus> checkResources(ResourceType type, OpenStackContext context, AuthenticatedContext auth,
            Iterable<CloudResource> resources) {
        List<CloudResourceStatus> result = new ArrayList<>();
        for (CloudResource resource : resources) {
            LOGGER.debug("Check {} resource: {}", type, resource);
            try {
                boolean finished = checkStatus(context, auth, resource);
                ResourceStatus successStatus = context.isBuild() ? ResourceStatus.CREATED : ResourceStatus.DELETED;
                result.add(new CloudResourceStatus(resource, finished ? successStatus : ResourceStatus.IN_PROGRESS));
                if (finished) {
                    if (successStatus == ResourceStatus.CREATED) {
                        LOGGER.debug("Creation of {} was successful", resource);
                    } else {
                        LOGGER.debug("Deletion of {} was successful", resource);
                    }
                }
            } catch (OS4JException ex) {
                throw new OpenStackResourceException("Error during status check", type, resource.getName(), ex);
            }
        }
        return result;
    }

    protected CloudResource checkDeleteResponse(ActionResponse response, ResourceType resourceType, AuthenticatedContext auth, CloudResource resource,
            String faultMsg) {
        if (!response.isSuccess()) {
            LOGGER.warn("OpenStack Action for resource [type: {}] was not successful! [status: {}, msg: {}]", resourceType,
                    StatusCode.fromCode(response.getCode()), response.getFault());
            if (response.getCode() != StatusCode.NOT_FOUND.getCode()) {
                throw new OpenStackResourceException(faultMsg, resourceType, resource.getName(), auth.getCloudContext().getId(),
                        response.getFault());
            } else {
                LOGGER.warn("OpenStack Action for resource [type: {}] was not NOT_FOUND (404) hence null will be returned from " +
                                "{}.checkDeleteResponse() method!", resourceType, AbstractOpenStackResourceBuilder.class.getSimpleName());
                return null;
            }
        }
        LOGGER.info("OpenStack Action for resource [type: {}] was successful!", resourceType);
        return resource;
    }

    @Override
    public Platform platform() {
        return OpenStackConstants.OPENSTACK_PLATFORM;
    }

    @Override
    public Variant variant() {
        return OpenStackVariant.NATIVE.variant();
    }

    protected abstract boolean checkStatus(OpenStackContext context, AuthenticatedContext auth, CloudResource resource);
}
