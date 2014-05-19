package com.sequenceiq.provisioning.converter;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.CloudInstanceRequest;
import com.sequenceiq.provisioning.domain.AzureCloudInstance;

@Component
public class AzureCloudConverter extends AbstractConverter<CloudInstanceRequest, AzureCloudInstance> {

    private static final String NAME = "name";
    private static final String LOCATION = "location";
    private static final String DESCRIPTION = "description";
    private static final String ADDRESSPREFIX = "addressPrefix";
    private static final String DEPLOYMENTSLOT = "deploymentSlot";
    private static final String IMAGENAME = "imageName";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String DISABLESSHPASSWORDAUTHENTICATION = "disableSshPasswordAuthentication";
    private static final String VMTYPE = "vmType";

    @Override
    public CloudInstanceRequest convert(AzureCloudInstance entity) {
      /*  InfraRequest azureStackJson = new InfraRequest();
        azureStackJson.setClusterName(entity.getName());
        azureStackJson.setCloudPlatform(CloudPlatform.AZURE);
        Map<String, String> props = new HashMap<>();
        putProperty(props, NAME, entity.getName());
        putProperty(props, LOCATION, entity.getLocation());
        putProperty(props, DESCRIPTION, entity.getDescription());
        putProperty(props, ADDRESSPREFIX, entity.getSubnetAddressPrefix());
        putProperty(props, DEPLOYMENTSLOT, entity.getDeploymentSlot());
        putProperty(props, IMAGENAME, entity.getImageName());
        putProperty(props, VMTYPE, entity.getVmType());
        putProperty(props, IMAGENAME, entity.getImageName());
        putProperty(props, DISABLESSHPASSWORDAUTHENTICATION, entity.getDisableSshPasswordAuthentication());
        putProperty(props, USERNAME, entity.getUserName());
        putProperty(props, PASSWORD, entity.getPassword());
        azureStackJson.setParameters(props);*/
        return new CloudInstanceRequest();
    }

    @Override
    public AzureCloudInstance convert(CloudInstanceRequest json) {
       /* AzureInfra azureInfra = new AzureInfra();
        azureInfra.setName(json.getClusterName());*/
        return new AzureCloudInstance();
    }

    private void putProperty(Map<String, String> props, String key, Object property) {
        if (property != null) {
            props.put(key, property.toString());
        }
    }

}
