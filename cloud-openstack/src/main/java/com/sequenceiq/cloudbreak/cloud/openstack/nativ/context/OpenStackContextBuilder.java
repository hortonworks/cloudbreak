package com.sequenceiq.cloudbreak.cloud.openstack.nativ.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;
import com.sequenceiq.cloudbreak.cloud.template.ResourceContextBuilder;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class OpenStackContextBuilder implements ResourceContextBuilder<OpenStackContext> {
    public static final int PARALLEL_RESOURCE_REQUEST = 30;
    @Inject
    private OpenStackClient openStackClient;

    @Override
    public OpenStackContext contextInit(CloudContext cloudContext, AuthenticatedContext auth, CloudStack cloudStack, boolean build) {
        OSClient osClient = openStackClient.createOSClient(auth);
        KeystoneCredentialView credentialView = new KeystoneCredentialView(auth.getCloudCredential());
        OpenStackContext openStackContext = initContext(cloudContext, cloudStack, build);
        openStackContext.putParameter(OpenStackConstants.TENANT_ID, osClient.identity().tenants().getByName(credentialView.getTenantName()).getId());
        return openStackContext;
    }

    @Override
    public OpenStackContext terminationContextInit(CloudContext cloudContext, AuthenticatedContext auth, CloudStack cloudStack, List<CloudResource> resources) {
        OpenStackContext osContext = initContext(cloudContext, cloudStack, false);
        for (CloudResource resource : resources) {
            if (resource.getType().equals(ResourceType.OPENSTACK_SUBNET)) {
                osContext.putParameter(OpenStackConstants.SUBNET_ID, resource.getReference());
            }
        }
        osContext.putParameter(OpenStackConstants.FLOATING_IP_IDS, Collections.synchronizedList(new ArrayList<String>()));
        return osContext;
    }

    private OpenStackContext initContext(CloudContext context, CloudStack cloudStack, boolean build) {
        return new OpenStackContext(context.getName(), cloudStack.getRegion(), PARALLEL_RESOURCE_REQUEST, build);
    }

    @Override
    public String platform() {
        return OpenStackConstants.OPENSTACK;
    }
}
