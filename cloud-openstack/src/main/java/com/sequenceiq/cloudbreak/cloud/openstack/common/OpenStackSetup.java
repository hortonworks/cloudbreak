package com.sequenceiq.cloudbreak.cloud.openstack.common;

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
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.FileSystem;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient;
import com.sequenceiq.cloudbreak.common.type.ImageStatus;
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult;

@Component
public class OpenStackSetup implements Setup {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackSetup.class);

    @Inject
    private OpenStackClient openStackClient;

    @Override
    public void prepareImage(AuthenticatedContext authenticatedContext, CloudStack stack, com.sequenceiq.cloudbreak.cloud.model.Image image) {
        String imageName = image.getImageName();
        OSClient osClient = openStackClient.createOSClient(authenticatedContext);
        verifyImage(osClient, imageName);
    }

    @Override
    public ImageStatusResult checkImageStatus(AuthenticatedContext authenticatedContext, CloudStack stack, com.sequenceiq.cloudbreak.cloud.model.Image image) {
        return new ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED);
    }

    @Override
    public void prerequisites(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier) {
        OSClient osClient = openStackClient.createOSClient(authenticatedContext);
        verifyFlavors(osClient, stack.getGroups());
        LOGGER.debug("setup has been executed");
    }

    @Override
    public void validateFileSystem(FileSystem fileSystem) throws Exception {
    }

    private void verifyFlavors(OSClient osClient, List<Group> instanceGroups) {
        List<? extends Flavor> flavors = osClient.compute().flavors().list();
        Set<String> notFoundFlavors = new HashSet<>();
        for (Group instanceGroup : instanceGroups) {
            String instanceType = instanceGroup.getInstances().get(0).getTemplate().getFlavor();
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
        List<? extends Image> images = osClient.images().listAll();
        for (Image image : images) {
            if (name.equalsIgnoreCase(image.getName())) {
                return;
            }
        }
        throw new CloudConnectorException(String.format("OpenStack image: %s not found", name));
    }
}
