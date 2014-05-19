package com.sequenceiq.provisioning.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.InfraRequest;
import com.sequenceiq.provisioning.controller.validation.RequiredAzureInfraParam;
import com.sequenceiq.provisioning.domain.AzureInfra;
import com.sequenceiq.provisioning.domain.CloudPlatform;

@Component
public class AzureInfraConverter extends AbstractConverter<InfraRequest, AzureInfra> {

    @Override
    public InfraRequest convert(AzureInfra entity) {
        InfraRequest azureStackJson = new InfraRequest();
        azureStackJson.setClusterName(entity.getName());
        azureStackJson.setCloudPlatform(CloudPlatform.AZURE);
        azureStackJson.setId(entity.getId());
        Map<String, String> props = new HashMap<>();
        putProperty(props, RequiredAzureInfraParam.NAME.getName(), entity.getName());
        putProperty(props, RequiredAzureInfraParam.LOCATION.getName(), entity.getLocation());
        putProperty(props, RequiredAzureInfraParam.DESCRIPTION.getName(), entity.getDescription());
        putProperty(props, RequiredAzureInfraParam.ADDRESSPREFIX.getName(), entity.getSubnetAddressPrefix());
        putProperty(props, RequiredAzureInfraParam.DEPLOYMENTSLOT.getName(), entity.getDeploymentSlot());
        putProperty(props, RequiredAzureInfraParam.IMAGENAME.getName(), entity.getImageName());
        putProperty(props, RequiredAzureInfraParam.VMTYPE.getName(), entity.getVmType());
        putProperty(props, RequiredAzureInfraParam.DISABLESSHPASSWORDAUTHENTICATION.getName(), entity.getDisableSshPasswordAuthentication());
        putProperty(props, RequiredAzureInfraParam.USERNAME.getName(), entity.getUserName());
        putProperty(props, RequiredAzureInfraParam.PASSWORD.getName(), entity.getPassword());
        azureStackJson.setParameters(props);
        return azureStackJson;
    }

    @Override
    public AzureInfra convert(InfraRequest json) {
        AzureInfra azureInfra = new AzureInfra();
        azureInfra.setName(json.getClusterName());
        azureInfra.setDeploymentSlot(json.getParameters().get(RequiredAzureInfraParam.DEPLOYMENTSLOT.getName()));
        azureInfra.setDescription(json.getParameters().get(RequiredAzureInfraParam.DESCRIPTION.getName()));
        azureInfra.setDisableSshPasswordAuthentication(Boolean.valueOf(json.getParameters()
                .get(RequiredAzureInfraParam.DISABLESSHPASSWORDAUTHENTICATION.getName())));
        azureInfra.setImageName(json.getParameters().get(RequiredAzureInfraParam.IMAGENAME.getName()));
        azureInfra.setLocation(json.getParameters().get(RequiredAzureInfraParam.LOCATION.getName()));
        azureInfra.setName(json.getClusterName());
        azureInfra.setPassword(json.getParameters().get(RequiredAzureInfraParam.PASSWORD.getName()));
        azureInfra.setSubnetAddressPrefix(json.getParameters().get(RequiredAzureInfraParam.ADDRESSPREFIX.getName()));
        azureInfra.setUserName(json.getParameters().get(RequiredAzureInfraParam.USERNAME.getName()));
        azureInfra.setVmType(json.getParameters().get(RequiredAzureInfraParam.VMTYPE.getName()));
        return azureInfra;
    }

    private void putProperty(Map<String, String> props, String key, Object property) {
        if (property != null) {
            props.put(key, property.toString());
        }
    }

}
