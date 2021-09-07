package com.sequenceiq.freeipa.service.stack.instance;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static java.util.Map.entry;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceGroupParameters;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.GcpInstanceTemplate;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.freeipa.api.model.ResourceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.DefaultRootVolumeSizeProvider;

@Service
public class DefaultInstanceGroupProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultInstanceGroupProvider.class);

    private static final int DEFAULT_FAULT_DOMAIN_COUNTER = 2;

    private static final int DEFAULT_UPDATE_DOMAIN_COUNTER = 20;

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Inject
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Inject
    private DefaultInstanceTypeProvider defaultInstanceTypeProvider;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    public Template createDefaultTemplate(CloudPlatform cloudPlatform, String accountId, String diskEncryptionSetId, String gcpKmsEncryptionKey) {
        Template template = new Template();
        template.setName(missingResourceNameGenerator.generateName(APIResourceType.TEMPLATE));
        template.setStatus(ResourceStatus.DEFAULT);
        template.setRootVolumeSize(defaultRootVolumeSizeProvider.getForPlatform(cloudPlatform.name()));
        template.setVolumeCount(0);
        template.setVolumeSize(0);
        template.setInstanceType(defaultInstanceTypeProvider.getForPlatform(cloudPlatform.name()));
        template.setAccountId(accountId);
        if (cloudPlatform == AWS) {
            // FIXME Enable EBS encryption with appropriate KMS key
            template.setAttributes(new Json(Map.<String, Object>ofEntries(
                    entry(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, Boolean.TRUE),
                    entry(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.DEFAULT.name()))));
        }
        if (cloudPlatform == AZURE && diskEncryptionSetId != null) {
            template.setAttributes(new Json(Map.<String, Object>ofEntries(
                    entry(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID, diskEncryptionSetId),
                    entry(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, Boolean.TRUE))));
        }
        if (cloudPlatform == CloudPlatform.GCP && gcpKmsEncryptionKey != null) {
            template.setAttributes(new Json(Map.<String, Object>ofEntries(
                    entry(GcpInstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, gcpKmsEncryptionKey),
                    entry(GcpInstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM),
                    entry(GcpInstanceTemplate.KEY_ENCRYPTION_METHOD, KeyEncryptionMethod.KMS))));
        }
        return template;
    }

    public Json createAttributes(CloudPlatform cloudPlatform, String stackName, String instanceGroupName) {
        if (cloudPlatform == AZURE) {
            Map<String, Object> asParameters = Map.ofEntries(
                    entry(AzureInstanceGroupParameters.NAME, String.format("%s-%s-as", stackName, instanceGroupName)),
                    entry(AzureInstanceGroupParameters.FAULT_DOMAIN_COUNT, DEFAULT_FAULT_DOMAIN_COUNTER),
                    entry(AzureInstanceGroupParameters.UPDATE_DOMAIN_COUNT, DEFAULT_UPDATE_DOMAIN_COUNTER));
            return new Json(Map.of("availabilitySet", asParameters));
        } else {
            return null;
        }
    }

    public InstanceGroupNetwork createDefaultNetwork(CloudPlatform cloudPlatform, NetworkRequest networkRequest) {
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        instanceGroupNetwork.setCloudPlatform(cloudPlatform.name());
        if (cloudPlatform == AZURE) {
            String subnetId = networkRequest.getAzure().getSubnetId();
            instanceGroupNetwork.setAttributes(getAttributes(subnetId));
        } else if (cloudPlatform == AWS) {
            String subnetId = networkRequest.getAws().getSubnetId();
            instanceGroupNetwork.setAttributes(getAttributes(subnetId));
        } else if (cloudPlatform == GCP) {
            String subnetId = networkRequest.getGcp().getSubnetId();
            instanceGroupNetwork.setAttributes(getAttributes(subnetId));
        }
        return instanceGroupNetwork;
    }

    private Json getAttributes(String subnetId) {
        return new Json(Map.of(NetworkConstants.SUBNET_IDS, Set.of(subnetId)));
    }
}
