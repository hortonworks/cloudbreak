package com.sequenceiq.cloudbreak.service.stack.resource.azure;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureStartStopContextObject;

import groovyx.net.http.HttpResponseException;

public abstract class AzureSimpleInstanceResourceBuilder implements
        ResourceBuilder<AzureProvisionContextObject, AzureDeleteContextObject, AzureDescribeContextObject, AzureStartStopContextObject> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AzureSimpleInstanceResourceBuilder.class);
    protected static final int POLLING_INTERVAL = 5000;
    protected static final int MAX_POLLING_ATTEMPTS = 60;
    protected static final int MAX_NAME_LENGTH = 50;
    protected static final int MAX_ATTEMPTS_FOR_AMBARI_OPS = -1;
    protected static final int NOT_FOUND = 404;
    protected static final int VALID_IP_RANGE_START = 4;
    protected static final String DESCRIPTION = "description";
    protected static final String AFFINITYGROUP = "affinityGroup";
    protected static final String DEPLOYMENTSLOT = "deploymentSlot";
    protected static final String LABEL = "label";
    protected static final String IMAGENAME = "imageName";
    protected static final String IMAGESTOREURI = "imageStoreUri";
    protected static final String HOSTNAME = "hostname";
    protected static final String USERNAME = "username";
    protected static final String SUBNETNAME = "subnetName";
    protected static final String VIRTUAL_NETWORK_IP_ADDRESS = "virtualNetworkIPAddress";
    protected static final String CUSTOMDATA = "customData";
    protected static final String VIRTUALNETWORKNAME = "virtualNetworkName";
    protected static final String VMTYPE = "vmType";
    protected static final String SSHPUBLICKEYFINGERPRINT = "sshPublicKeyFingerprint";
    protected static final String SSHPUBLICKEYPATH = "sshPublicKeyPath";
    protected static final String PORTS = "ports";
    protected static final String DISKS = "disks";
    protected static final String DATA = "data";
    protected static final String DEFAULT_USER_NAME = "ubuntu";
    protected static final String PRODUCTION = "production";

    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public ResourceBuilderType resourceBuilderType() {
        return ResourceBuilderType.INSTANCE_RESOURCE;
    }

    public String getVmName(String azureTemplate, int i) {
        return String.format("%s-%s", azureTemplate, i);
    }

    protected List<Resource> filterResourcesByType(List<Resource> resources, ResourceType resourceType) {
        List<Resource> resourcesTemp = new ArrayList<>();
        for (Resource resource : resources) {
            if (resourceType.equals(resource.getResourceType())) {
                resourcesTemp.add(resource);
            }
        }
        return resourcesTemp;
    }

    protected void httpResponseExceptionHandler(HttpResponseException ex, String resourceName, String user, Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        if (ex.getStatusCode() != NOT_FOUND) {
            throw new InternalServerException(ex.getMessage());
        } else {
            LOGGER.error(String.format("Azure resource not found with %s name for %s user.", resourceName, user));
        }
    }

    protected void waitUntilComplete(AzureClient azureClient, String requestId) {
        boolean finished = azureClient.waitUntilComplete(requestId);
        if (!finished) {
            throw new InternalServerException("Azure resource timeout");
        }
    }

    @Override
    public Boolean start(AzureStartStopContextObject aSSCO, Resource resource) {
        return true;
    }

    @Override
    public Boolean stop(AzureStartStopContextObject aSSCO, Resource resource) {
        return true;
    }

    @Override
    public Boolean rollback(Resource resource, AzureDeleteContextObject azureDeleteContextObject) throws Exception {
        return delete(resource, azureDeleteContextObject);
    }
}
