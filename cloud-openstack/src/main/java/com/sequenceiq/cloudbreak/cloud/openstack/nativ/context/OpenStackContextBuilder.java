package com.sequenceiq.cloudbreak.cloud.openstack.nativ.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;
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
        OSClient osClient = openStackClient.createOSClient(auth);
        KeystoneCredentialView credentialView = new KeystoneCredentialView(auth);

        OpenStackContext openStackContext = new OpenStackContext(utils.getStackName(auth), cloudContext.getLocation(),
                PARALLEL_RESOURCE_REQUEST, build);

        openStackContext.putParameter(OpenStackConstants.TENANT_ID, osClient.identity().tenants().getByName(credentialView.getTenantName()).getId());

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
                        openStackContext.addGroupResources(resource.getGroup(), Arrays.asList(resource));
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
        return OpenStackConstants.OpenStackVariant.NATIVE.variant();
    }
}
