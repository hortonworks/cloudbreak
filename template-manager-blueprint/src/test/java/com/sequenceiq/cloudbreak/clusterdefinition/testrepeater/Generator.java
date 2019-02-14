package com.sequenceiq.cloudbreak.clusterdefinition.testrepeater;

import org.junit.rules.TestRule;

public interface Generator<T> extends TestRule {

    T value();

}
