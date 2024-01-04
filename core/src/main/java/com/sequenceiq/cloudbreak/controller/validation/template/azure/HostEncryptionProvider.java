package com.sequenceiq.cloudbreak.controller.validation.template.azure;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.environment.api.v1.environment.endpoint.service.azure.HostEncryptionCalculator;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class HostEncryptionProvider {

    @Inject
    private TemplateService templateService;

    @Inject
    private HostEncryptionCalculator hostEncryptionCalculator;

    public Template updateWithHostEncryption(DetailedEnvironmentResponse environmentResponse, Credential credential, Template template, VmType vmType) {
        if (credential.cloudPlatform().equalsIgnoreCase(CloudPlatform.AZURE.name())) {
            if (vmType == null) {
                throw new BadRequestException("The virtual machine type for Azure is probably not supported in your subscription. " +
                        "Please make sure you select an instance which is enabled for CDP.");
            }
            Json attributesJson = template.getAttributes();
            if (hostEncryptionCalculator.hostEncryptionRequired(environmentResponse)) {
                Map<String, Object> attributes = Optional.ofNullable(attributesJson).map(Json::getMap).orElseGet(HashMap::new);
                attributes.put(AzureInstanceTemplate.ENCRYPTION_AT_HOST_ENABLED, vmType.getMetaData().getHostEncryptionSupported());
                template.setAttributes(new Json(attributes));
                return templateService.savePure(template);
            }
        }
        return template;
    }
}
