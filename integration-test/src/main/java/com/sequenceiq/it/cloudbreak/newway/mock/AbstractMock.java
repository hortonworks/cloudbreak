package com.sequenceiq.it.cloudbreak.newway.mock;

import spark.Service;

public abstract class AbstractMock {
    private final Service sparkService;

    public AbstractMock(Service sparkService) {
        this.sparkService = sparkService;
    }

    public Service getSparkService() {
        return sparkService;
    }

}
