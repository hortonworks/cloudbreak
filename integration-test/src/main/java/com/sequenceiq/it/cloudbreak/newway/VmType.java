package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class VmType extends VmTypeEntity {
    static Function<IntegrationTestContext, VmType> getTestContext(String key) {
        return (testContext) -> testContext.getContextParam(key, VmType.class);
    }

    static Function<IntegrationTestContext, VmType> getNew() {
        return (testContext) -> new VmType();
    }

    public static VmType request() {
        return new VmType();
    }

    public static Action<VmType> getPlatformVmTypes(String key) {
        return new Action<>(getTestContext(key), VmTypeAction::getVmTypesByCredentialId);
    }

    public static Action<VmType> getPlatformVmTypes() {
        return getPlatformVmTypes(VMTYPE);
    }

    public static Assertion<VmType> assertThis(BiConsumer<VmType, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}
