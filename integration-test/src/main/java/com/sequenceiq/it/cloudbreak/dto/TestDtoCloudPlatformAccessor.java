package com.sequenceiq.it.cloudbreak.dto;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public class TestDtoCloudPlatformAccessor extends AbstractTestDto {
    private AbstractTestDto testDto;

    private TestDtoCloudPlatformAccessor(String newId) {
        super(newId);
    }

    public TestDtoCloudPlatformAccessor(AbstractTestDto testDto) {
        super("id");
        this.testDto = testDto;
    }

    @Override
    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        testDto.setCloudPlatform(cloudPlatform);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return testDto.getCloudPlatform();
    }
}


