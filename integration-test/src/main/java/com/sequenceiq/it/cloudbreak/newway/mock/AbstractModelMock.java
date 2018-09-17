package com.sequenceiq.it.cloudbreak.newway.mock;

import spark.Service;

public abstract class AbstractModelMock extends AbstractMock {
    private final DefaultModel defaultModel;

    public AbstractModelMock(Service sparkService, DefaultModel defaultModel) {
        super(sparkService);
        this.defaultModel = defaultModel;
    }

    public DefaultModel getDefaultModel() {
        return defaultModel;
    }

}
