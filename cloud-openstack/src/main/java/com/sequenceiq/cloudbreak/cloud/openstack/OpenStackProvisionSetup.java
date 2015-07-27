package com.sequenceiq.cloudbreak.cloud.openstack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.cloud.ProvisionSetup;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;

@Component("OpenStackProvisionSetupV2")
public class OpenStackProvisionSetup implements ProvisionSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackProvisionSetup.class);

    @Inject
    private OpenStackClient openStackClient;

    @Override
    public Map<String, Object> setup(AuthenticatedContext authenticatedContext, CloudStack stack) throws Exception {
        return new HashMap<>();
    }

    @Override
    public String preCheck(AuthenticatedContext authenticatedContext, CloudStack stack) {
        String imageName = stack.getImage().getImageName();
        OSClient osClient = openStackClient.createOSClient(authenticatedContext);
        Optional<String> flavor = verifyFlavors(osClient, stack.getGroups());
        if (flavor.isPresent()) {
            return flavor.get();
        }
        Optional<String> image = verifyImage(osClient, imageName);
        if (image.isPresent()) {
            return image.get();
        }
        return null;
    }

    private Optional<String> verifyFlavors(OSClient osClient, List<Group> instanceGroups) {
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
