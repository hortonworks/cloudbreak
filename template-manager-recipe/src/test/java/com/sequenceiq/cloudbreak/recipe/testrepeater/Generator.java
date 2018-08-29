package com.sequenceiq.cloudbreak.recipe.testrepeater;

import org.junit.rules.TestRule;

public interface Generator<T> extends TestRule {

    T value();

}
