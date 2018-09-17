package com.sequenceiq.it.cloudbreak.newway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.verification.Call;
import com.sequenceiq.it.verification.Verification;

import spark.Response;

public class AssertMock extends Assertion<Mock> {
    private static List<Verification> verificationArray = new ArrayList<>();

    AssertMock(Function<IntegrationTestContext, Mock> entitySupplier, BiConsumer<Mock, IntegrationTestContext> check) {
        super(entitySupplier, check);
    }

    AssertMock(Verification[] verifications) {
        super(Mock.getTestContextMock(), AssertMock::checkVerifications);
        setVerificationArray(Arrays.asList(verifications));
    }

    private static void setVerificationArray(List<Verification> verifications) {
        verificationArray = verifications;
    }

    private static void checkVerifications(Mock mock, IntegrationTestContext integrationTestContext) {
        Map<Call, Response> requestResponseMap = mock.getRequestResponseMap();
        Objects.requireNonNull(verificationArray, "There is no verification to do").stream()
                .forEach(verification -> verification.verify(requestResponseMap));
    }
}
