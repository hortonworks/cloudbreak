package com.sequenceiq.cloudbreak.blueprint.testrepeater;

import org.junit.rules.TestRule;

public interface Generator<T> extends TestRule {

    T value();

}
