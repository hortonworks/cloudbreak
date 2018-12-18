package com.sequenceiq.it.cloudbreak.newway.entity;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.CustomInstanceType;
import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.template.AwsParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.AzureParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.OpenStackParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.YarnParameters;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class TemplateEntity extends AbstractCloudbreakEntity<TemplateV2Request, TemplateResponse, TemplateEntity> {

    public TemplateEntity(TemplateV2Request request, TestContext testContext) {
        super(request, testContext);
    }

    public TemplateEntity(TestContext testContext) {
        super(new TemplateV2Request(), testContext);
    }

    public TemplateEntity() {
        super(TemplateEntity.class.getSimpleName().toUpperCase());
    }

    public TemplateEntity valid() {
        return withInstanceType("large")
                .withVolumeCount(1)
                .withVolumeSize(100)
                .withVolumeType("magnetic");
    }

    public TemplateEntity withAwsParameters(AwsParameters awsParameters) {
        getRequest().setAwsParameters(awsParameters);
        return this;
    }

    public TemplateEntity withGcpParameters(GcpParameters gcpParameters) {
        getRequest().setGcpParameters(gcpParameters);
        return this;
    }

    public TemplateEntity withAzureParameters(AzureParameters azureParameters) {
        getRequest().setAzureParameters(azureParameters);
        return this;
    }

    public TemplateEntity withOpenStackParameters(OpenStackParameters openStackParameters) {
        getRequest().setOpenStackParameters(openStackParameters);
        return this;
    }

    public TemplateEntity withVolumeSize(Integer volumeSize) {
        getRequest().setVolumeSize(volumeSize);
        return this;
    }

    public TemplateEntity withVolumeCount(Integer volumeCount) {
        getRequest().setVolumeCount(volumeCount);
        return this;
    }

    public TemplateEntity withParameters(Map<String, Object> parameters) {
        getRequest().setParameters(parameters);
        return this;
    }

    public TemplateEntity withVolumeType(String volumeType) {
        getRequest().setVolumeType(volumeType);
        return this;
    }

    public TemplateEntity withInstanceType(String instanceType) {
        getRequest().setInstanceType(instanceType);
        return this;
    }

    public TemplateEntity withCustomInstanceType(CustomInstanceType customInstanceType) {
        getRequest().setCustomInstanceType(customInstanceType);
        return this;
    }

    public TemplateEntity withRootVolumeSize(Integer rootVolumeSize) {
        getRequest().setRootVolumeSize(rootVolumeSize);
        return this;
    }

    public TemplateEntity withYarnParameters(YarnParameters yarnParameters) {
        getRequest().setYarnParameters(yarnParameters);
        return this;
    }
}
