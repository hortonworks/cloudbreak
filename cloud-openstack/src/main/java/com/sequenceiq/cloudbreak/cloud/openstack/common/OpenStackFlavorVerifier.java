package com.sequenceiq.cloudbreak.cloud.openstack.common;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Group;

@Component
public class OpenStackFlavorVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackFlavorVerifier.class);

    public void flavorsExist(OSClient<?> osClient, Iterable<Group> instanceGroups) {
        if (instanceGroups == null) {
            throw new CloudConnectorException("Cannot validate Flavors if InstanceGroup is null");
        }
        List<? extends Flavor> flavors = osClient.compute().flavors().list();
        if (flavors == null || flavors.isEmpty()) {
            throw new CloudConnectorException("No flavor found on OpenStack");
        }
        Set<String> notFoundFlavors = new HashSet<>();
        for (Group instanceGroup : instanceGroups) {
            String instanceType = instanceGroup.getReferenceInstanceTemplate().getFlavor();
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
            LOGGER.info("Not found flavors: {}", notFoundFlavors);
            throw new CloudConnectorException(String.format("Not found flavors: %s", notFoundFlavors));
        }
    }
}
