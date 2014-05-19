package com.sequenceiq.provisioning.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.InfraRequest;
import com.sequenceiq.provisioning.domain.AzureInfra;
import com.sequenceiq.provisioning.domain.CloudPlatform;

@Component
public class AzureInfraConverter extends AbstractConverter<InfraRequest, AzureInfra> {

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
    public InfraRequest convert(AzureInfra entity) {
        InfraRequest azureStackJson = new InfraRequest();
        azureStackJson.setClusterName(entity.getName());
        azureStackJson.setCloudPlatform(CloudPlatform.AZURE);
        azureStackJson.setId(entity.getId());
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
        azureStackJson.setParameters(props);
        return azureStackJson;
    }

    @Override
    public AzureInfra convert(InfraRequest json) {
        AzureInfra azureInfra = new AzureInfra();
        azureInfra.setName(json.getClusterName());
        azureInfra.setDeploymentSlot(json.getParameters().get(DEPLOYMENTSLOT));
        azureInfra.setDescription(json.getParameters().get(DESCRIPTION));
        azureInfra.setDisableSshPasswordAuthentication(Boolean.valueOf(json.getParameters().get(DISABLESSHPASSWORDAUTHENTICATION)));
        azureInfra.setImageName(json.getParameters().get(IMAGENAME));
        azureInfra.setLocation(json.getParameters().get(LOCATION));
        azureInfra.setName(json.getClusterName());
        azureInfra.setPassword(json.getParameters().get(PASSWORD));
        azureInfra.setSubnetAddressPrefix(json.getParameters().get(ADDRESSPREFIX));
        azureInfra.setUserName(json.getParameters().get(USERNAME));
        azureInfra.setVmType(json.getParameters().get(VMTYPE));
        return azureInfra;
    }

    private void putProperty(Map<String, String> props, String key, Object property) {
        if (property != null) {
            props.put(key, property.toString());
        }
    }

}
