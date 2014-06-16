package com.sequenceiq.cloudbreak.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.controller.validation.AzureTemplateParam;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Port;

@Component
public class AzureTemplateConverter extends AbstractConverter<TemplateJson, AzureTemplate> {

    @Override
    public TemplateJson convert(AzureTemplate entity) {
        TemplateJson azureTemplateJson = new TemplateJson();
        azureTemplateJson.setName(entity.getName());
        azureTemplateJson.setCloudPlatform(CloudPlatform.AZURE);
        azureTemplateJson.setId(entity.getId());
        Map<String, Object> props = new HashMap<>();
        putProperty(props, AzureTemplateParam.LOCATION.getName(), entity.getLocation());
        putProperty(props, AzureTemplateParam.DESCRIPTION.getName(), entity.getDescription());
        putProperty(props, AzureTemplateParam.ADDRESSPREFIX.getName(), entity.getSubnetAddressPrefix());
        putProperty(props, AzureTemplateParam.DEPLOYMENTSLOT.getName(), entity.getDeploymentSlot());
        putProperty(props, AzureTemplateParam.IMAGENAME.getName(), entity.getImageName());
        putProperty(props, AzureTemplateParam.VMTYPE.getName(), entity.getVmType());
        putProperty(props, AzureTemplateParam.USERNAME.getName(), entity.getUserName());
        putProperty(props, AzureTemplateParam.PASSWORD.getName(), entity.getPassword());
        putProperty(props, AzureTemplateParam.SUBNETADDRESSPREFIX.getName(), entity.getSubnetAddressPrefix());
        props.put(AzureTemplateParam.PORTS.getName(), entity.getPorts());
        azureTemplateJson.setCloudPlatform(CloudPlatform.AZURE);
        azureTemplateJson.setParameters(props);
        azureTemplateJson.setDescription(entity.getDescription() == null ? "" : entity.getDescription());
        return azureTemplateJson;
    }

    @Override
    public AzureTemplate convert(TemplateJson json) {
        AzureTemplate azureTemplate = new AzureTemplate();
        azureTemplate.setName(json.getName());
        azureTemplate.setDeploymentSlot(String.valueOf(json.getParameters().get(AzureTemplateParam.DEPLOYMENTSLOT.getName())));
        azureTemplate.setDescription(String.valueOf(json.getParameters().get(AzureTemplateParam.DESCRIPTION.getName())));
        azureTemplate.setImageName(String.valueOf(json.getParameters().get(AzureTemplateParam.IMAGENAME.getName())));
        azureTemplate.setLocation(String.valueOf(json.getParameters().get(AzureTemplateParam.LOCATION.getName())));
        azureTemplate.setName(String.valueOf(json.getName()));
        azureTemplate.setPassword(String.valueOf(json.getParameters().get(AzureTemplateParam.PASSWORD.getName())));
        azureTemplate.setAddressPrefix(String.valueOf(json.getParameters().get(AzureTemplateParam.ADDRESSPREFIX.getName())));
        azureTemplate.setUserName(String.valueOf(json.getParameters().get(AzureTemplateParam.USERNAME.getName())));
        azureTemplate.setVmType(String.valueOf(json.getParameters().get(AzureTemplateParam.VMTYPE.getName())));
        azureTemplate.setSubnetAddressPrefix(String.valueOf(json.getParameters().get(AzureTemplateParam.SUBNETADDRESSPREFIX.getName())));
        Set<Port> ports = new HashSet<>();
        Object portObject = json.getParameters().get(AzureTemplateParam.PORTS.getName());
        if (portObject != null) {
            for (Map<String, String> portEntry : (ArrayList<LinkedHashMap<String, String>>) portObject) {
                Port port = new Port();
                port.setLocalPort(portEntry.get("localPort"));
                port.setName(portEntry.get("name"));
                port.setPort(portEntry.get("port"));
                port.setProtocol(portEntry.get("protocol"));
                port.setAzureTemplate(azureTemplate);
                ports.add(port);
            }
        }
        azureTemplate.setDescription(json.getDescription());
        azureTemplate.setPorts(ports);
        return azureTemplate;
    }

    private void putProperty(Map<String, Object> props, String key, Object property) {
        if (property != null) {
            props.put(key, property.toString());
        }
    }

}
