package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.VmTypeV3Action;

public class VmType extends VmTypeEntity {
    static Function<IntegrationTestContext, VmType> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, VmType.class);
    }

    static Function<IntegrationTestContext, VmType> getNew() {
        return testContext -> new VmType();
    }

    public static VmType request() {
        return new VmType();
    }

    public static ResourceAction<VmType> getPlatformVmTypes(String key) {
        return new ResourceAction<>(getTestContext(key), VmTypeV3Action::getVmTypesByCredentialId);
    }

    public static ResourceAction<VmType> getPlatformVmTypes() {
        return getPlatformVmTypes(VMTYPE);
    }

    public static Assertion<VmType> assertThis(BiConsumer<VmType, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}
