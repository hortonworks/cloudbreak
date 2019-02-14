package com.sequenceiq.cloudbreak.clusterdefinition.testrepeater;

import org.junit.rules.ErrorCollector;

public class AccessibleErrorCollector extends ErrorCollector {

    @Override
    public void verify() throws Throwable {
        super.verify();
    }

}