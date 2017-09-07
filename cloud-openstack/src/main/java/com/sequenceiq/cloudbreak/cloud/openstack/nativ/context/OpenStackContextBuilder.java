package com.sequenceiq.cloudbreak.cloud.openstack.nativ.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.OpenStackVariant;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils;
import com.sequenceiq.cloudbreak.cloud.template.ResourceContextBuilder;

@Service
public class OpenStackContextBuilder implements ResourceContextBuilder<OpenStackContext> {
    private static final int PARALLEL_RESOURCE_REQUEST = 30;

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackContextBuilder.class);

    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private OpenStackUtils utils;

    @Override
    public OpenStackContext contextInit(CloudContext cloudContext, AuthenticatedContext auth, Network network, List<CloudResource> resources, boolean build) {
        OpenStackContext openStackContext = new OpenStackContext(utils.getStackName(auth), cloudContext.getLocation(),
                PARALLEL_RESOURCE_REQUEST, build);

        if (openStackClient.isV2Keystone(auth)) {
            String v2TenantId = openStackClient.getV2TenantId(auth);
            openStackContext.putParameter(OpenStackConstants.TENANT_ID, v2TenantId);
        } else {
            throw new CloudConnectorException("In case on native openstack api only V2 keystone is supported");
        }

        if (resources != null) {
            for (CloudResource resource : resources) {
                switch (resource.getType()) {
                    case OPENSTACK_SUBNET:
                        openStackContext.putParameter(OpenStackConstants.SUBNET_ID, resource.getReference());
                        break;
                    case OPENSTACK_NETWORK:
                        openStackContext.putParameter(OpenStackConstants.NETWORK_ID, resource.getReference());
                        break;
                    case OPENSTACK_SECURITY_GROUP:
                        openStackContext.addGroupResources(resource.getGroup(), Collections.singletonList(resource));
                        break;
                    default:
                        LOGGER.debug("Resource is not used during context build: {}", resource);
                }
            }
        }
        openStackContext.putParameter(OpenStackConstants.FLOATING_IP_IDS, Collections.synchronizedList(new ArrayList<String>()));
        if (network != null) {
            openStackContext.putParameter(OpenStackConstants.PUBLIC_NET_ID, network.getStringParameter(OpenStackConstants.PUBLIC_NET_ID));
        }

        return openStackContext;
    }

    @Override
    public Platform platform() {
        return OpenStackConstants.OPENSTACK_PLATFORM;
    }

    @Override
    public Variant variant() {
        return OpenStackVariant.NATIVE.variant();
    }
}
