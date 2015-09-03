package com.sequenceiq.cloudbreak.cloud.openstack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;

@Component
public class OpenStackSetup implements Setup {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackSetup.class);

    @Inject
    private OpenStackClient openStackClient;

    @Override
    public void execute(AuthenticatedContext authenticatedContext, CloudStack stack) {
        String imageName = stack.getImage().getImageName();
        OSClient osClient = openStackClient.createOSClient(authenticatedContext);
        verifyFlavors(osClient, stack.getGroups());
        verifyImage(osClient, imageName);
        LOGGER.debug("setup has been executed");
    }

    private void verifyFlavors(OSClient osClient, List<Group> instanceGroups) {
        List<? extends Flavor> flavors = osClient.compute().flavors().list();
        Set<String> notFoundFlavors = new HashSet<>();
        for (Group instanceGroup : instanceGroups) {
            String instanceType = instanceGroup.getInstances().get(0).getFlavor();
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

        if (!notFoundFlavors.isEmpty()) {
            throw new CloudConnectorException(String.format("Not found flavors: %s", notFoundFlavors));
        }
    }

    private void verifyImage(OSClient osClient, String name) {
        List<? extends Image> images = osClient.images().list();
        for (Image image : images) {
            if (name.equalsIgnoreCase(image.getName())) {
                return;
            }
        }
        throw new CloudConnectorException(String.format("OpenStack image: %s not found", name));
    }
}
