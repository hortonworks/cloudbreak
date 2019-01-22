package com.sequenceiq.it.cloudbreak.newway.entity;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateParametersV4;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class AzureInstanceTemplateParametersV4Entity extends AbstractCloudbreakEntity<AzureInstanceTemplateParametersV4, AzureInstanceTemplateParametersV4,
        AzureInstanceTemplateParametersV4Entity> {

    protected AzureInstanceTemplateParametersV4Entity(TestContext testContext) {
        super(new AzureInstanceTemplateParametersV4(), testContext);
    }
}
