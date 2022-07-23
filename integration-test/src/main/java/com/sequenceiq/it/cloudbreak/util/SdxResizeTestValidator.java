package com.sequenceiq.it.cloudbreak.util;

import java.util.concurrent.atomic.AtomicReference;

import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

public class SdxResizeTestValidator {
    private final SdxClusterShape expectedShape;

    private final AtomicReference<String> expectedCrn;

    private final AtomicReference<String> expectedName;

    private final AtomicReference<String> expectedRuntime;

    public SdxResizeTestValidator(SdxClusterShape expectedShape) {
        this.expectedShape = expectedShape;
        expectedCrn = new AtomicReference<>();
        expectedName = new AtomicReference<>();
        expectedRuntime = new AtomicReference<>();
    }

    public void setExpectedCrn(String expectedCrn) {
        this.expectedCrn.set(expectedCrn);
    }

    public void setExpectedName(String expectedName) {
        this.expectedName.set(expectedName);
    }

    public void setExpectedRuntime(String expectedRuntime) {
        this.expectedRuntime.set(expectedRuntime);
    }

    public SdxInternalTestDto validateResizedCluster(SdxInternalTestDto dto) {
        SdxClusterResponse response = dto.getResponse();
        validateClusterShape(response.getClusterShape());
        validateCrn(response.getCrn());
        validateStackCrn(response.getStackCrn());
        validateName(response.getName());
        validateRuntime(response.getRuntime());
        return dto;
    }

    private void validateClusterShape(SdxClusterShape shape) {
        if (!expectedShape.equals(shape)) {
            fail("cluster shape", expectedShape.name(), shape.name());
        }
    }

    private void validateCrn(String crn) {
        if (!expectedCrn.get().equals(crn)) {
            fail("crn", expectedCrn.get(), crn);
        }
    }

    private void validateStackCrn(String stackCrn) {
        if (!expectedCrn.get().equals(stackCrn)) {
            fail("stack crn", expectedCrn.get(), stackCrn);
        }
    }

    private void validateName(String name) {
        if (!expectedName.get().equals(name)) {
            fail("name", expectedName.get(), name);
        }
    }

    private void validateRuntime(String runtime) {
        if (!expectedRuntime.get().equals(runtime)) {
            fail("runtime", expectedRuntime.get(), runtime);
        }
    }

    private void fail(String testField, String expected, String actual) {
        throw new TestFailException(
                " The DL " + testField + " is '" + actual + "' instead of '" + expected + '\''
        );
    }
}
