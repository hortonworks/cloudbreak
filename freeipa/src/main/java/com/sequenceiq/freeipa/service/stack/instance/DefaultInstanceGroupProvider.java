package com.sequenceiq.freeipa.service.stack.instance;

import static java.util.Map.entry;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceGroupParameters;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.freeipa.api.model.ResourceStatus;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.DefaultRootVolumeSizeProvider;

@Service
public class DefaultInstanceGroupProvider {

    private static final int DEFAULT_FAULT_DOMAIN_COUNTER = 2;

    private static final int DEFAULT_UPDATE_DOMAIN_COUNTER = 20;

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Inject
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Inject
    private DefaultInstanceTypeProvider defaultInstanceTypeProvider;

    @Inject
    private EntitlementService entitlementService;

    public Template createDefaultTemplate(CloudPlatform cloudPlatform, String accountId) {
        Template template = new Template();
        template.setName(missingResourceNameGenerator.generateName(APIResourceType.TEMPLATE));
        template.setStatus(ResourceStatus.DEFAULT);
        template.setRootVolumeSize(defaultRootVolumeSizeProvider.getForPlatform(cloudPlatform.name()));
        template.setVolumeCount(0);
        template.setVolumeSize(0);
        template.setInstanceType(defaultInstanceTypeProvider.getForPlatform(cloudPlatform.name()));
        template.setAccountId(accountId);
        if (cloudPlatform == CloudPlatform.AWS) {
            // FIXME Enable EBS encryption with appropriate KMS key
            template.setAttributes(new Json(Map.<String, Object>ofEntries(
                    entry(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, Boolean.TRUE),
                    entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.DEFAULT.name()))));
        }
        return template;
    }

    public Json createAttributes(CloudPlatform cloudPlatform, String stackName, String instanceGroupName) {
        if (cloudPlatform == CloudPlatform.AZURE) {
            Map<String, Object> asParameters = Map.ofEntries(
                    entry(AzureInstanceGroupParameters.NAME, String.format("%s-%s-as", stackName, instanceGroupName)),
                    entry(AzureInstanceGroupParameters.FAULT_DOMAIN_COUNT, DEFAULT_FAULT_DOMAIN_COUNTER),
                    entry(AzureInstanceGroupParameters.UPDATE_DOMAIN_COUNT, DEFAULT_UPDATE_DOMAIN_COUNTER));
            return new Json(Map.of("availabilitySet", asParameters));
        } else {
            return null;
        }
    }
}
