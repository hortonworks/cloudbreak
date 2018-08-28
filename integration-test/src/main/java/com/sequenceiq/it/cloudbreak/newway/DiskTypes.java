package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.DiskTypesV3Action;

public class DiskTypes extends DiskTypesEntity {

    static Function<IntegrationTestContext, DiskTypes> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, DiskTypes.class);
    }

    static Function<IntegrationTestContext, DiskTypes> getNew() {
        return testContext -> new DiskTypes();
    }

    public static DiskTypes request() {
        return new DiskTypes();
    }

    public static DiskTypes isCreated() {
        DiskTypes diskTypes = new DiskTypes();
        diskTypes.setCreationStrategy(DiskTypesAction::createInGiven);
        return diskTypes;
    }

    public static Action<DiskTypes> get(String key) {
        return new Action<>(getTestContext(key), DiskTypesV3Action::get);
    }

    public static Action<DiskTypes> get() {
        return get(DISKTYPES);
    }

    public static Action<DiskTypes> getByType(String key) {
        return new Action<>(getTestContext(key), DiskTypesV3Action::getByType);
    }

    public static Action<DiskTypes> getByType() {
        return getByType(DISKTYPES);
    }

    public static Assertion<DiskTypes> assertThis(BiConsumer<DiskTypes, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}
