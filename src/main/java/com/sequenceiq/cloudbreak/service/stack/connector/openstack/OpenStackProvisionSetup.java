package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.CREDENTIAL;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.OpenStackTemplate;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionEvent;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;

import reactor.core.Reactor;

@Component
public class OpenStackProvisionSetup implements ProvisionSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackProvisionSetup.class);

    @Autowired
    private Reactor reactor;

    @Autowired
    private OpenStackUtil openStackUtil;

    @Override
    public ProvisionEvent setupProvisioning(Stack stack) throws Exception {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT, stack.getId());
        return new ProvisionSetupComplete(getCloudPlatform(), stack.getId())
                .withSetupProperty(CREDENTIAL, stack.getCredential());
    }

    @Override
    public String preProvisionCheck(Stack stack) {
        String imageName = stack.getImage();
        OSClient osClient = openStackUtil.createOSClient(stack);
        Optional<String> result = verifyFlavors(osClient, stack.getInstanceGroupsAsList());
        return result.isPresent() ? result.get() : verifyImage(osClient, imageName).get();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }

    private Optional<String> verifyFlavors(OSClient osClient, List<InstanceGroup> instanceGroups) {
        List<? extends Flavor> flavors = osClient.compute().flavors().list();
        Set<String> notFoundFlavors = new HashSet<>();
        for (InstanceGroup instanceGroup : instanceGroups) {
            String instanceType = ((OpenStackTemplate) instanceGroup.getTemplate()).getInstanceType();
            boolean found = false;
            for (Flavor flavor : flavors) {
                if (flavor.getName().equalsIgnoreCase(instanceType)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                notFoundFlavors.add(instanceType);
            }
        }
        return notFoundFlavors.isEmpty() ? Optional.<String>absent() : Optional.of(String.format("Not found flavors: %s", notFoundFlavors));
    }

    private Optional<String> verifyImage(OSClient osClient, String name) {
        List<? extends Image> images = osClient.images().list();
        for (Image image : images) {
            if (name.equalsIgnoreCase(image.getName())) {
                return Optional.absent();
            }
        }
        return Optional.of(String.format(String.format("OpenStack image: %s not found", name)));
    }

}
