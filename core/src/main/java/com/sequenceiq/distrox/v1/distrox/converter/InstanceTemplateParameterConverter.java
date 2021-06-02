package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4SpotParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsPlacementGroupV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.YarnInstanceTemplateV4Parameters;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsEncryptionV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1SpotParameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsPlacementGroupV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AzureInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.GcpInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.YarnInstanceTemplateV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class InstanceTemplateParameterConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTemplateParameterConverter.class);

    public AwsInstanceTemplateV4Parameters convert(AwsInstanceTemplateV1Parameters source) {
        AwsInstanceTemplateV4Parameters response = new AwsInstanceTemplateV4Parameters();
        response.setEncryption(getIfNotNull(source.getEncryption(), this::convert));
        response.setSpot(getIfNotNull(source.getSpot(), this::convert));
        response.setPlacementGroup(getIfNotNull(source.getPlacementGroup(), this::convert));
        return response;
    }

    private AwsInstanceTemplateV4SpotParameters convert(AwsInstanceTemplateV1SpotParameters source) {
        AwsInstanceTemplateV4SpotParameters target = new AwsInstanceTemplateV4SpotParameters();
        target.setPercentage(source.getPercentage());
        target.setMaxPrice(source.getMaxPrice());
        return target;
    }

    private AwsEncryptionV4Parameters convert(AwsEncryptionV1Parameters source) {
        AwsEncryptionV4Parameters response = new AwsEncryptionV4Parameters();
        response.setKey(source.getKey());
        response.setType(source.getType());
        return response;
    }

    private AwsPlacementGroupV4Parameters convert(AwsPlacementGroupV1Parameters source) {
        AwsPlacementGroupV4Parameters response = new AwsPlacementGroupV4Parameters();
        response.setStrategy(source.getStrategy());
        return response;
    }

    public GcpInstanceTemplateV4Parameters convert(GcpInstanceTemplateV1Parameters source) {
        return new GcpInstanceTemplateV4Parameters();
    }

    public GcpInstanceTemplateV1Parameters convert(GcpInstanceTemplateV4Parameters source) {
        return new GcpInstanceTemplateV1Parameters();
    }

    public AzureInstanceTemplateV4Parameters convert(AzureInstanceTemplateV1Parameters source, DetailedEnvironmentResponse environment) {
        AzureInstanceTemplateV4Parameters response = new AzureInstanceTemplateV4Parameters();
        response.setEncrypted(source.getEncrypted());
        initAzureEncryptionFromEnvironment(response, environment);
        response.setManagedDisk(source.getManagedDisk());
        response.setPrivateId(source.getPrivateId());
        return response;
    }

    private void initAzureEncryptionFromEnvironment(AzureInstanceTemplateV4Parameters response, DetailedEnvironmentResponse environment) {
        String diskEncryptionSetId = Optional.of(environment)
                .map(DetailedEnvironmentResponse::getAzure)
                .map(AzureEnvironmentParameters::getResourceEncryptionParameters)
                .map(AzureResourceEncryptionParameters::getDiskEncryptionSetId)
                .orElse(null);
        if (diskEncryptionSetId != null) {
            LOGGER.info("Applying SSE with CMK for Azure managed disks as per environment.");
            AzureEncryptionV4Parameters encryption = new AzureEncryptionV4Parameters();
            encryption.setType(EncryptionType.CUSTOM);
            encryption.setDiskEncryptionSetId(diskEncryptionSetId);
            response.setEncryption(encryption);
        } else {
            LOGGER.info("Environment has not requested for SSE with CMK for Azure managed disks.");
        }
    }

    public YarnInstanceTemplateV4Parameters convert(YarnInstanceTemplateV1Parameters source) {
        YarnInstanceTemplateV4Parameters response = new YarnInstanceTemplateV4Parameters();
        response.setCpus(source.getCpus());
        response.setMemory(source.getMemory());
        return response;
    }

    public AwsInstanceTemplateV1Parameters convert(AwsInstanceTemplateV4Parameters source) {
        AwsInstanceTemplateV1Parameters response = new AwsInstanceTemplateV1Parameters();
        response.setEncryption(getIfNotNull(source.getEncryption(), this::convert));
        response.setSpot(getIfNotNull(source.getSpot(), this::convert));
        response.setPlacementGroup(getIfNotNull(source.getPlacementGroup(), this::convert));
        return response;
    }

    private AwsEncryptionV1Parameters convert(AwsEncryptionV4Parameters source) {
        AwsEncryptionV1Parameters response = new AwsEncryptionV1Parameters();
        response.setKey(source.getKey());
        response.setType(source.getType());
        return response;
    }

    private AwsPlacementGroupV1Parameters convert(AwsPlacementGroupV4Parameters source) {
        AwsPlacementGroupV1Parameters response = new AwsPlacementGroupV1Parameters();
        response.setStrategy(source.getStrategy());
        return response;
    }

    private AwsInstanceTemplateV1SpotParameters convert(AwsInstanceTemplateV4SpotParameters source) {
        AwsInstanceTemplateV1SpotParameters awsInstanceGroupV1SpotParameters = new AwsInstanceTemplateV1SpotParameters();
        awsInstanceGroupV1SpotParameters.setPercentage(source.getPercentage());
        awsInstanceGroupV1SpotParameters.setMaxPrice(source.getMaxPrice());
        return awsInstanceGroupV1SpotParameters;
    }

    public AzureInstanceTemplateV1Parameters convert(AzureInstanceTemplateV4Parameters source) {
        AzureInstanceTemplateV1Parameters response = new AzureInstanceTemplateV1Parameters();
        response.setEncrypted(source.getEncrypted());
        response.setManagedDisk(source.getManagedDisk());
        response.setPrivateId(source.getPrivateId());
        return response;
    }

    public YarnInstanceTemplateV1Parameters convert(YarnInstanceTemplateV4Parameters source) {
        YarnInstanceTemplateV1Parameters response = new YarnInstanceTemplateV1Parameters();
        response.setCpus(source.getCpus());
        response.setMemory(source.getMemory());
        return response;
    }
}
