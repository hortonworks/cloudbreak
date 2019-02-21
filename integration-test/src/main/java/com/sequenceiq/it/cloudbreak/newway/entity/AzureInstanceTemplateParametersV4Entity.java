package com.sequenceiq.it.cloudbreak.newway.entity;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateV4Parameters;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class AzureInstanceTemplateParametersV4Entity extends AbstractCloudbreakEntity<AzureInstanceTemplateV4Parameters, AzureInstanceTemplateV4Parameters,
        AzureInstanceTemplateParametersV4Entity> {

    protected AzureInstanceTemplateParametersV4Entity(TestContext testContext) {
        super(new AzureInstanceTemplateV4Parameters(), testContext);
    }
}
