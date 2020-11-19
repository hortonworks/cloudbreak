package com.sequenceiq.mock.clouderamanager;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

@Service
public class DefaultModelService {

    public static final String PROFILE_RETURN_HTTP_500 = "cmHttp500";

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultModelService.class);

    private static final int SSH_PORT = 2020;

    private static final int IP_PART_MAX = 256;

    @Value("${mock.infrastructure.host:localhost}")
    private String mockInfrastructureHost;

    @PostConstruct
    public void init() {
        LOGGER.info("Mock-infrastructure host: {} ", mockInfrastructureHost);
    }

    public List<CloudVmMetaDataStatus> createInstances(String name, List<Group> groups) {
        List<CloudVmMetaDataStatus> ret = new ArrayList<>();
        String prefix = "192.168";
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            Group group = groups.get(groupIndex);
            for (int instanceIndex = 0; instanceIndex < group.getInstances().size(); instanceIndex++) {
                CloudInstance cloudInstance = group.getInstances().get(instanceIndex);
                String address = String.format("%s.%s.%s", prefix, groupIndex % IP_PART_MAX, cloudInstance.getTemplate().getPrivateId() % IP_PART_MAX);
                String instanceId = String.format("instance-%s", address);
                CloudInstance cloudInstanceWithId = new CloudInstance(instanceId, getTemplateCreated(cloudInstance), cloudInstance.getAuthentication());
                CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstanceWithId, InstanceStatus.STARTED);
                String publicIp = mockInfrastructureHost + ":10090/" + name;
                CloudInstanceMetaData cloudInstanceMetaData = new CloudInstanceMetaData(address, publicIp, SSH_PORT, "MOCK");
                CloudVmMetaDataStatus cloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, cloudInstanceMetaData);
                ret.add(cloudVmMetaDataStatus);
            }
        }
        return ret;
    }

    private InstanceTemplate getTemplateCreated(CloudInstance cloudInstance) {
        InstanceTemplate template = cloudInstance.getTemplate();
        return new InstanceTemplate(template.getFlavor(), template.getGroupName(), template.getPrivateId(), template.getVolumes(), InstanceStatus.CREATED,
                template.getParameters(), template.getTemplateId(), template.getImageId());
    }
}
