package com.sequenceiq.mock.spi;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.mock.clouderamanager.DefaultModelService;

@Service
public class SpiStoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpiStoreService.class);

    @Inject
    private DefaultModelService defaultModelService;

    private final Map<String, SpiDto> spiDtoMap = new ConcurrentHashMap<>();

    public List<CloudVmMetaDataStatus> store(String mockUuid, CloudStack cloudStack) {
        LOGGER.info("New CloudStack will be created for {}, {}", mockUuid, cloudStack);
        List<CloudVmMetaDataStatus> instances = defaultModelService.createInstances(mockUuid, cloudStack.getGroups());
        SpiDto spiDto = new SpiDto(mockUuid, cloudStack);
        spiDto.setVmMetaDataStatuses(instances);
        spiDtoMap.put(mockUuid, spiDto);
        return instances;
    }

    public SpiDto read(String mockUuid) {
        LOGGER.info("Fetch spi by {}", mockUuid);
        SpiDto spiDto = spiDtoMap.get(mockUuid);
        if (spiDto == null) {
            LOGGER.info("Cannot find any spi by {}", mockUuid);
            throw new ResponseStatusException(NOT_FOUND, "SpiDto cannot be found by uuid: " + mockUuid);
        }
        return spiDto;
    }

    public List<CloudVmMetaDataStatus> getMetadata(String mockUuid) {
        LOGGER.info("Get metadata for {}", mockUuid);
        SpiDto spiDto = read(mockUuid);
        return spiDto.getVmMetaDataStatuses();
    }

    public void remove(String mockUuid, CloudVmMetaDataStatus cloudVmMetaDataStatus) {
        LOGGER.info("remove {} from {}", cloudVmMetaDataStatus, mockUuid);
        read(mockUuid).getVmMetaDataStatuses().remove(cloudVmMetaDataStatus);
    }

    public CloudVmMetaDataStatus modifyInstanceStatus(String mockUuid, CloudVmMetaDataStatus metaDataStatus, InstanceStatus instanceStatus) {
        LOGGER.info("Modify instance for {} with new status: {}, instance: {}", mockUuid, instanceStatus, instanceStatus);
        SpiDto spiDto = read(mockUuid);
        CloudVmInstanceStatus currentInstance = metaDataStatus.getCloudVmInstanceStatus();
        CloudVmInstanceStatus newInstanceStatus = new CloudVmInstanceStatus(currentInstance.getCloudInstance(), instanceStatus);
        CloudVmMetaDataStatus newVmMetaData = new CloudVmMetaDataStatus(newInstanceStatus, metaDataStatus.getMetaData());
        spiDto.getVmMetaDataStatuses().remove(metaDataStatus);
        spiDto.getVmMetaDataStatuses().add(newVmMetaData);
        return newVmMetaData;
    }

    public List<CloudVmMetaDataStatus> resize(String mockUuid, List<Group> groups) {
        LOGGER.info("Resize {}", mockUuid);
        List<CloudVmMetaDataStatus> instances = defaultModelService.createInstances(mockUuid, groups);
        LOGGER.info("{} will be resized with {}", mockUuid, instances);
        read(mockUuid).getVmMetaDataStatuses().addAll(instances);
        return instances;
    }

    public void terminate(String mockUuid) {
        LOGGER.info("Terminate {}", mockUuid);
        spiDtoMap.remove(mockUuid);
    }

    public Collection<SpiDto> getAll() {
        LOGGER.info("List all spi");
        return spiDtoMap.values();
    }
}
