package com.sequenceiq.mock.clouderamanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.mock.spi.SpiDto;

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

    public List<CloudVmMetaDataStatus> createInstances(String name, SpiDto spiDto, List<Group> groups) {
        List<CloudVmMetaDataStatus> ret = new ArrayList<>();
        String prefix = "192";
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            List<Group> existedGroups = spiDto.getCloudStack().getGroups();
            Group group = groups.get(groupIndex);
            Optional<Group> existedGroupOpt = existedGroups.stream().filter(g -> group.getName().equals(g.getName())).findFirst();
            if (existedGroupOpt.isEmpty()) {
                throw new NotFoundException("Cannot find group with name: " + group.getName());
            }
            Group existedGroup = existedGroupOpt.get();
            int indexOfExistedGroup = existedGroups.indexOf(existedGroup);
            for (int instanceIndex = 0; instanceIndex < group.getInstances().size(); instanceIndex++) {
                CloudInstance cloudInstance = group.getInstances().get(instanceIndex);
                String address = generateAddress(prefix, spiDto, indexOfExistedGroup, ret);
                String instanceId = String.format("instance-%s", address);
                CloudInstance cloudInstanceWithId = new CloudInstance(
                        instanceId,
                        getTemplateCreated(cloudInstance),
                        cloudInstance.getAuthentication(),
                        cloudInstance.getSubnetId(),
                        cloudInstance.getAvailabilityZone());
                CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstanceWithId, InstanceStatus.STARTED);
                String publicIp = mockInfrastructureHost + ":10090/" + name;
                CloudInstanceMetaData cloudInstanceMetaData = new CloudInstanceMetaData(address, publicIp, SSH_PORT, "MOCK");
                CloudVmMetaDataStatus cloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, cloudInstanceMetaData);
                ret.add(cloudVmMetaDataStatus);
            }
        }
        return ret;
    }

    String generateAddress(String prefix, SpiDto spiDto, long groupIndex, List<CloudVmMetaDataStatus> current) {
        String formatPattern = "%s.%s.%s.%s";
        int count = 0;
        String address;
        do {
            int ipPart1 = count / IP_PART_MAX;
            int ipPart2 = count % IP_PART_MAX;
            address = String.format(formatPattern, prefix, groupIndex, ipPart1, ipPart2);
            count++;
        } while (isAddressExisted(address, spiDto.getVmMetaDataStatuses()) || isAddressExisted(address, current));
        return address;
    }

    private boolean isAddressExisted(String address, List<CloudVmMetaDataStatus> vmStatuses) {
        return vmStatuses.stream()
                .anyMatch(vm -> vm.getMetaData().getPrivateIp().equals(address));
    }

    private InstanceTemplate getTemplateCreated(CloudInstance cloudInstance) {
        InstanceTemplate template = cloudInstance.getTemplate();
        return new InstanceTemplate(template.getFlavor(), template.getGroupName(), template.getPrivateId(), template.getVolumes(), InstanceStatus.CREATED,
                template.getParameters(), template.getTemplateId(), template.getImageId(), template.getTemporaryStorage(), template.getTemporaryStorageCount());
    }
}
