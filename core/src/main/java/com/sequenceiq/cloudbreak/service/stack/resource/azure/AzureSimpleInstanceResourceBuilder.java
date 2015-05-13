package com.sequenceiq.cloudbreak.service.stack.resource.azure;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureResourceException;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;
import com.sequenceiq.cloudbreak.service.stack.resource.UpdateContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureStartStopContextObject;

import groovyx.net.http.HttpResponseException;

public abstract class AzureSimpleInstanceResourceBuilder implements
        ResourceBuilder<AzureProvisionContextObject, AzureDeleteContextObject, AzureStartStopContextObject, UpdateContextObject> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AzureSimpleInstanceResourceBuilder.class);
    protected static final int POLLING_INTERVAL = 5000;
    protected static final int MAX_POLLING_ATTEMPTS = 60;
    protected static final int MAX_NAME_LENGTH = 50;
    protected static final int MAX_ATTEMPTS_FOR_AMBARI_OPS = -1;
    protected static final int NOT_FOUND = 404;
    protected static final String DESCRIPTION = "description";
    protected static final String AFFINITYGROUP = "affinityGroup";
    protected static final String DEPLOYMENTSLOT = "deploymentSlot";
    protected static final String LABEL = "label";
    protected static final String IMAGENAME = "imageName";
    protected static final String IMAGESTOREURI = "imageStoreUri";
    protected static final String HOSTNAME = "hostname";
    protected static final String USERNAME = "username";
    protected static final String SUBNETNAME = "subnetName";
    protected static final String CUSTOMDATA = "customData";
    protected static final String VIRTUALNETWORKNAME = "virtualNetworkName";
    protected static final String VMTYPE = "vmType";
    protected static final String SSHPUBLICKEYFINGERPRINT = "sshPublicKeyFingerprint";
    protected static final String SSHPUBLICKEYPATH = "sshPublicKeyPath";
    protected static final String DISKS = "disks";
    protected static final String DATA = "data";
    protected static final String PRODUCTION = "production";

    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public ResourceBuilderType resourceBuilderType() {
        return ResourceBuilderType.INSTANCE_RESOURCE;
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

    protected String getVmName(String azureTemplate, int i) {
        return String.format("%s-%s", azureTemplate, i);
    }

    protected void httpResponseExceptionHandler(HttpResponseException ex, String resourceName, String user, Stack stack) {
        if (ex.getStatusCode() != NOT_FOUND) {
            throw new AzureResourceException(ex.getResponse().getData().toString());
        } else {
            LOGGER.error(String.format("Azure resource not found with %s name for %s user.", resourceName, user));
        }
    }

    @Override
    public void update(UpdateContextObject updateContextObject) {
    }

    @Override
    public Boolean start(AzureStartStopContextObject startStopContextObject, Resource resource, String region) {
        return true;
    }

    @Override
    public Boolean stop(AzureStartStopContextObject startStopContextObject, Resource resource, String region) {
        return true;
    }

    @Override
    public Boolean rollback(Resource resource, AzureDeleteContextObject deleteContextObject, String region) throws Exception {
        return delete(resource, deleteContextObject, region);
    }
}
