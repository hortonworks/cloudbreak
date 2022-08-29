package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4SpotParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsPlacementGroupV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpEncryptionV4Parameters;
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
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsDiskEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class InstanceTemplateParameterConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTemplateParameterConverter.class);

    public AwsInstanceTemplateV4Parameters convert(AwsInstanceTemplateV1Parameters source, DetailedEnvironmentResponse environment) {
        AwsInstanceTemplateV4Parameters response = new AwsInstanceTemplateV4Parameters();
        if (environment != null) {
            response.setEncryption(getIfNotNull(source.getEncryption(), environment, this::convert));
        }
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

    private AwsEncryptionV4Parameters convert(AwsEncryptionV1Parameters source, DetailedEnvironmentResponse environment) {
        AwsEncryptionV4Parameters response = new AwsEncryptionV4Parameters();
        String dataHubEncryptionKey = source.getKey();
        EncryptionType dataHubEncryptionKeyType = source.getType();
        if (EncryptionType.CUSTOM.equals(dataHubEncryptionKeyType)) {
            response.setKey(dataHubEncryptionKey);
            response.setType(dataHubEncryptionKeyType);
        } else {
            String environmentEncryptionKeyArn = Optional.ofNullable(environment)
                    .map(DetailedEnvironmentResponse::getAws)
                    .map(AwsEnvironmentParameters::getAwsDiskEncryptionParameters)
                    .map(AwsDiskEncryptionParameters::getEncryptionKeyArn)
                    .orElse(null);
            if (environmentEncryptionKeyArn != null && !environmentEncryptionKeyArn.isEmpty()) {
                LOGGER.info("Applying AWS CMK for instance volume encryption as per environment configuration.");
                response.setKey(environmentEncryptionKeyArn);
                response.setType(EncryptionType.CUSTOM);
            } else {
                response.setType(EncryptionType.DEFAULT);
                LOGGER.info("Environment configuration does not contain AWS CMK for instance volume encryption. Using default encryption.");
            }
        }
        return response;
    }

    private AwsPlacementGroupV4Parameters convert(AwsPlacementGroupV1Parameters source) {
        AwsPlacementGroupV4Parameters response = new AwsPlacementGroupV4Parameters();
        response.setStrategy(source.getStrategy());
        return response;
    }

    public GcpInstanceTemplateV4Parameters convert(GcpInstanceTemplateV1Parameters source, DetailedEnvironmentResponse environment) {
        GcpInstanceTemplateV4Parameters response = new GcpInstanceTemplateV4Parameters();
        if (environment != null) {
            initGcpEncryptionFromEnvironment(response, environment);
        }
        return response;
    }

    public GcpInstanceTemplateV1Parameters convert(GcpInstanceTemplateV4Parameters source) {
        return new GcpInstanceTemplateV1Parameters();
    }

    public AzureInstanceTemplateV4Parameters convert(AzureInstanceTemplateV1Parameters source, DetailedEnvironmentResponse environment) {
        AzureInstanceTemplateV4Parameters response = new AzureInstanceTemplateV4Parameters();
        response.setEncrypted(source.getEncrypted());
        if (environment != null) {
            initAzureEncryptionFromEnvironment(response, environment);
        }
        response.setManagedDisk(source.getManagedDisk());
        response.setPrivateId(source.getPrivateId());
        return response;
    }

    private void initAzureEncryptionFromEnvironment(AzureInstanceTemplateV4Parameters response, DetailedEnvironmentResponse environment) {
        String encryptionKeyUrl = Optional.of(environment)
                .map(DetailedEnvironmentResponse::getAzure)
                .map(AzureEnvironmentParameters::getResourceEncryptionParameters)
                .map(AzureResourceEncryptionParameters::getEncryptionKeyUrl)
                .orElse(null);
        String diskEncryptionSetId = Optional.of(environment)
                .map(DetailedEnvironmentResponse::getAzure)
                .map(AzureEnvironmentParameters::getResourceEncryptionParameters)
                .map(AzureResourceEncryptionParameters::getDiskEncryptionSetId)
                .orElse(null);
        if (encryptionKeyUrl != null && diskEncryptionSetId != null) {
            LOGGER.info("Applying SSE with CMK for Azure managed disks as per environment.");
            AzureEncryptionV4Parameters encryption = new AzureEncryptionV4Parameters();
            encryption.setKey(encryptionKeyUrl);
            encryption.setType(EncryptionType.CUSTOM);
            encryption.setDiskEncryptionSetId(diskEncryptionSetId);
            response.setEncryption(encryption);
        } else {
            LOGGER.info("Environment has not requested for SSE with CMK for Azure managed disks.");
        }
    }

    private void initGcpEncryptionFromEnvironment(GcpInstanceTemplateV4Parameters response, DetailedEnvironmentResponse environment) {
        String encryptionKey = Optional.of(environment)
                .map(DetailedEnvironmentResponse::getGcp)
                .map(GcpEnvironmentParameters::getGcpResourceEncryptionParameters)
                .map(GcpResourceEncryptionParameters::getEncryptionKey)
                .orElse(null);
        if (encryptionKey != null) {
            LOGGER.info("Applying Encryption with CMEK for GCP disks as per environment.");
            GcpEncryptionV4Parameters encryption = new GcpEncryptionV4Parameters();
            encryption.setType(EncryptionType.CUSTOM);
            encryption.setKeyEncryptionMethod(KeyEncryptionMethod.KMS);
            encryption.setKey(encryptionKey);
            response.setEncryption(encryption);
            response.setEncrypted(Boolean.TRUE);
        } else {
            response.setEncrypted(Boolean.FALSE);
            LOGGER.info("Environment has not requested for Customer-Managed Encryption with CMEK for GCP disks.");
        }
    }

    public YarnInstanceTemplateV4Parameters convert(YarnInstanceTemplateV1Parameters source) {
        YarnInstanceTemplateV4Parameters response = new YarnInstanceTemplateV4Parameters();
        response.setCpus(source.getCpus());
        response.setMemory(source.getMemory());
        return response;
    }

    public AwsInstanceTemplateV1Parameters convert(AwsInstanceTemplateV4Parameters source, DetailedEnvironmentResponse environment) {
        AwsInstanceTemplateV1Parameters response = new AwsInstanceTemplateV1Parameters();
        response.setEncryption(getIfNotNull(source.getEncryption(), environment, this::convert));
        response.setSpot(getIfNotNull(source.getSpot(), this::convert));
        response.setPlacementGroup(getIfNotNull(source.getPlacementGroup(), this::convert));
        return response;
    }

    private AwsEncryptionV1Parameters convert(AwsEncryptionV4Parameters source, DetailedEnvironmentResponse environment) {
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
