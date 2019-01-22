package com.sequenceiq.it.cloudbreak.newway.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class StackTemplateEntity extends StackV4EntityBase<StackTemplateEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTemplateEntity.class);

    public StackTemplateEntity(TestContext testContext) {
        super(testContext);
    }
}
