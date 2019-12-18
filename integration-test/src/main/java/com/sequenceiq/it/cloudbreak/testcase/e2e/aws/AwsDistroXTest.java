package com.sequenceiq.it.cloudbreak.testcase.e2e.aws;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsEncryptionV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.ImageValidatorE2ETest;

public class AwsDistroXTest extends ImageValidatorE2ETest {

    @Inject
    private DistroXTestClient distroXTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIPA(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid DistroX create request is sent",
            then = "DistroX cluster is created")
    public void testCreateDistroX(TestContext testContext) {
        testContext.given(DistroXTestDto.class)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .then((context, distrox, client) -> {
                    distrox.getResponse();
                    return distrox;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid DistroX create request is sent with encrypted discs",
            then = "DistroX cluster is created")
    public void testCreateDistroXWithEncryptedVolume(TestContext testContext) {
        List<DistroXInstanceGroupTestDto> distroXInstanceGroupTestDtos = getInstanceGroupsWithDiscEncryption(testContext);
        testContext.given(DistroXTestDto.class)
                .withInstanceGroupsEntity(distroXInstanceGroupTestDtos)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .then((context, distrox, client) -> {
                    distrox.getResponse();
                    return distrox;
                })
                .validate();
    }

    private List<DistroXInstanceGroupTestDto> getInstanceGroupsWithDiscEncryption(TestContext testContext) {
        List<DistroXInstanceGroupTestDto> distroXInstanceGroupTestDtos = DistroXInstanceGroupTestDto.defaultHostGroup(testContext);
        List<InstanceTemplateV1Request> awsInstanceTemplates = getAwsInstanceTemplates(distroXInstanceGroupTestDtos);
        awsInstanceTemplates.forEach(instanceTemplate -> instanceTemplate.setAws(createAwsInstanceTemplateV1Parameters()));
        return distroXInstanceGroupTestDtos;
    }

    private List<InstanceTemplateV1Request> getAwsInstanceTemplates(List<DistroXInstanceGroupTestDto> distroXInstanceGroupTestDtos) {
        return distroXInstanceGroupTestDtos.stream()
                .map(this::getInstanceTemplate)
                .collect(Collectors.toList());
    }

    private AwsInstanceTemplateV1Parameters createAwsInstanceTemplateV1Parameters() {
        AwsInstanceTemplateV1Parameters awsInstanceTemplateV1Parameters = new AwsInstanceTemplateV1Parameters();
        AwsEncryptionV1Parameters awsEncryptionV1Parameters = new AwsEncryptionV1Parameters();
        awsEncryptionV1Parameters.setType(EncryptionType.DEFAULT);
        awsInstanceTemplateV1Parameters.setEncryption(awsEncryptionV1Parameters);
        return awsInstanceTemplateV1Parameters;
    }

    private InstanceTemplateV1Request getInstanceTemplate(DistroXInstanceGroupTestDto distroXInstanceGroupTestDto) {
        return distroXInstanceGroupTestDto.getRequest().getTemplate();
    }

    @Override
    protected String getImageId(TestContext testContext) {
        return testContext.get(DistroXTestDto.class).getResponse().getImage().getId();
    }
}
