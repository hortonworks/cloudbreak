package com.sequenceiq.cloudbreak.converter.spi;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.util.NullUtil.putIfPresent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.InstanceMetadataToImageIdConverter;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class InstanceMetaDataToCloudInstanceConverter {

    @Inject
    private StackToCloudStackConverter stackToCloudStackConverter;

    @Inject
    private InstanceMetadataToImageIdConverter instanceMetadataToImageIdConverter;

    @Inject
    private EnvironmentClientService environmentClientService;

    public List<CloudInstance> convert(Iterable<InstanceMetaData> metaDataEntities, String envCrn, StackAuthentication stackAuthentication) {
        List<CloudInstance> cloudInstances = new ArrayList<>();
        DetailedEnvironmentResponse environment = environmentClientService.getByCrnAsInternal(envCrn);
        for (InstanceMetaData metaDataEntity : metaDataEntities) {
            cloudInstances.add(convert(metaDataEntity, environment, stackAuthentication));
        }
        return cloudInstances;
    }

    public CloudInstance convert(InstanceMetaData metaDataEntity, DetailedEnvironmentResponse environment, StackAuthentication stackAuthentication) {
        InstanceGroup group = metaDataEntity.getInstanceGroup();
        Template template = group.getTemplate();
        InstanceStatus status = getInstanceStatus(metaDataEntity);
        String imageId = instanceMetadataToImageIdConverter.convert(metaDataEntity);
        InstanceTemplate instanceTemplate = stackToCloudStackConverter.buildInstanceTemplate(template, group.getGroupName(), metaDataEntity.getPrivateId(),
                status, imageId);
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication(
                stackAuthentication.getPublicKey(),
                stackAuthentication.getPublicKeyId(),
                stackAuthentication.getLoginUserName());
        Map<String, Object> params = new HashMap<>();
        putIfPresent(params, SUBNET_ID, metaDataEntity.getSubnetId());
        putIfPresent(params, CloudInstance.INSTANCE_NAME, metaDataEntity.getInstanceName());
        Map<String, Object> cloudInstanceParameters = stackToCloudStackConverter.buildCloudInstanceParameters(
                environment,
                metaDataEntity,
                CloudPlatform.valueOf(template.cloudPlatform()));
        params.putAll(cloudInstanceParameters);

        return new CloudInstance(
                metaDataEntity.getInstanceId(),
                instanceTemplate,
                instanceAuthentication,
                metaDataEntity.getSubnetId(),
                metaDataEntity.getAvailabilityZone(),
                params);
    }

    private InstanceStatus getInstanceStatus(InstanceMetaData metaData) {
        switch (metaData.getInstanceStatus()) {
            case REQUESTED:
                return InstanceStatus.CREATE_REQUESTED;
            case CREATED:
                return InstanceStatus.CREATED;
            case STARTED:
            case SERVICES_RUNNING:
            case SERVICES_HEALTHY:
            case SERVICES_UNHEALTHY:
                return InstanceStatus.STARTED;
            case DECOMMISSIONED:
            case DELETE_REQUESTED:
                return InstanceStatus.DELETE_REQUESTED;
            case TERMINATED:
                return InstanceStatus.TERMINATED;
            case DELETED_BY_PROVIDER:
                return InstanceStatus.TERMINATED_BY_PROVIDER;
            default:
                return InstanceStatus.UNKNOWN;
        }
    }

}
