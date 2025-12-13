package com.sequenceiq.mock.clouderamanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.mock.spi.SpiDto;

@ExtendWith(MockitoExtension.class)
public class DefaultModelServiceTest {

    @InjectMocks
    private DefaultModelService underTest;

    @Test
    public void testGenerateAddress() {
        CloudStack cloudStack = mock(CloudStack.class);
        SpiDto spiDto = new SpiDto("", cloudStack);
        String actual = underTest.generateAddress("192", spiDto, 1, Collections.emptyList());
        assertEquals("192.1.0.0", actual);
    }

    @Test
    public void testGenerateAddressWhenOnExistsAndShouldGenerateNewOne() {
        CloudStack cloudStack = mock(CloudStack.class);
        CloudVmInstanceStatus cloudVmInstanceStatus = mock(CloudVmInstanceStatus.class);
        SpiDto spiDto = new SpiDto("", cloudStack);
        spiDto.getVmMetaDataStatuses().add(new CloudVmMetaDataStatus(cloudVmInstanceStatus, new CloudInstanceMetaData("192.1.0.0", "1.1.1.1")));
        String actual = underTest.generateAddress("192", spiDto, 1, Collections.emptyList());
        assertEquals("192.1.0.1", actual);
    }

    @Test
    public void testGenerateAddressWhenExistsWithGapAndShouldGenerateNewOne() {
        CloudStack cloudStack = mock(CloudStack.class);
        CloudVmInstanceStatus cloudVmInstanceStatus = mock(CloudVmInstanceStatus.class);
        SpiDto spiDto = new SpiDto("", cloudStack);
        spiDto.getVmMetaDataStatuses().add(new CloudVmMetaDataStatus(cloudVmInstanceStatus, new CloudInstanceMetaData("192.1.0.0", "1.1.1.1")));
        spiDto.getVmMetaDataStatuses().add(new CloudVmMetaDataStatus(cloudVmInstanceStatus, new CloudInstanceMetaData("192.1.0.1", "1.1.1.1")));
        spiDto.getVmMetaDataStatuses().add(new CloudVmMetaDataStatus(cloudVmInstanceStatus, new CloudInstanceMetaData("192.1.0.4", "1.1.1.1")));
        String actual = underTest.generateAddress("192", spiDto, 1, Collections.emptyList());
        assertEquals("192.1.0.2", actual);
    }

    @Test
    public void testGenerateWithMoreThan255() {
        CloudStack cloudStack = mock(CloudStack.class);
        SpiDto spiDto = new SpiDto("", cloudStack);
        generate(spiDto, 256);

        String actual = underTest.generateAddress("192", spiDto, 1, Collections.emptyList());
        assertEquals("192.1.1.0", actual);
    }

    @Test
    public void testGenerateWithMoreThan255AndOneMissing() {
        CloudStack cloudStack = mock(CloudStack.class);
        SpiDto spiDto = new SpiDto("", cloudStack);
        generate(spiDto, 256);

        spiDto.getVmMetaDataStatuses().remove(10);

        String actual = underTest.generateAddress("192", spiDto, 1, Collections.emptyList());
        assertEquals("192.1.0.10", actual);
    }

    @Test
    public void testGenerateAddressWhenSpiDtoEmptyButListHasEntry() {
        CloudStack cloudStack = mock(CloudStack.class);
        SpiDto spiDto = new SpiDto("", cloudStack);
        CloudVmInstanceStatus cloudVmInstanceStatus = mock(CloudVmInstanceStatus.class);
        List<CloudVmMetaDataStatus> current = new ArrayList<>();
        current.add(new CloudVmMetaDataStatus(cloudVmInstanceStatus, new CloudInstanceMetaData("192.1.0.0", "1.1.1.1")));
        String actual = underTest.generateAddress("192", spiDto, 1, current);
        assertEquals("192.1.0.1", actual);
    }

    private void generate(SpiDto spiDto, int count) {
        for (int i = 0; i < count; i++) {
            CloudVmInstanceStatus cloudVmInstanceStatus = mock(CloudVmInstanceStatus.class);
            String address = underTest.generateAddress("192", spiDto, 1, Collections.emptyList());
            CloudVmMetaDataStatus cloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, new CloudInstanceMetaData(address, "1.1.1.1"));
            spiDto.getVmMetaDataStatuses().add(cloudVmMetaDataStatus);
        }
    }
}
