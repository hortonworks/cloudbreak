package com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.COMPUTE;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.WORKER;
import static com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto.withHostGroup;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsEncryptionV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public class DistroXInstanceGroupsBuilder {

    private final TestContext testContext;

    private List<DistroXInstanceGroupTestDto> distroXInstanceGroupTestDtoList;

    public DistroXInstanceGroupsBuilder(TestContext testContext) {
        this.testContext = testContext;
    }

    public DistroXInstanceGroupsBuilder defaultHostGroup() {
        distroXInstanceGroupTestDtoList = withHostGroup(testContext, MASTER, COMPUTE, WORKER);
        return this;
    }

    public DistroXInstanceGroupsBuilder withDiskEncryption() {
        getInstanceTemplates().forEach(this::setInstanceTemplateV1Parameters);
        return this;
    }

    public List<DistroXInstanceGroupTestDto> build() {
        return distroXInstanceGroupTestDtoList;
    }

    private List<InstanceTemplateV1Request> getInstanceTemplates() {
        return distroXInstanceGroupTestDtoList.stream()
                .map(this::getInstanceTemplate)
                .collect(Collectors.toList());
    }

    private InstanceTemplateV1Request getInstanceTemplate(DistroXInstanceGroupTestDto distroXInstanceGroupTestDto) {
        return distroXInstanceGroupTestDto.getRequest().getTemplate();
    }

    private void setInstanceTemplateV1Parameters(InstanceTemplateV1Request instanceTemplateV1Request) {
        CloudPlatform cloudPlatform = testContext.getCloudProvider().getCloudPlatform();
        switch (cloudPlatform) {
            case AWS:
                instanceTemplateV1Request.setAws(getAwsInstanceTemplateV1Parameters());
                break;
            default:
                throw new NotImplementedException(String.format("Not implemented on cloudPlatform %s", cloudPlatform));
        }
    }

    private AwsInstanceTemplateV1Parameters getAwsInstanceTemplateV1Parameters() {
        AwsInstanceTemplateV1Parameters awsInstanceTemplateV1Parameters = new AwsInstanceTemplateV1Parameters();
        AwsEncryptionV1Parameters awsEncryptionV1Parameters = new AwsEncryptionV1Parameters();
        awsEncryptionV1Parameters.setType(EncryptionType.DEFAULT);
        awsInstanceTemplateV1Parameters.setEncryption(awsEncryptionV1Parameters);
        return awsInstanceTemplateV1Parameters;
    }
}
