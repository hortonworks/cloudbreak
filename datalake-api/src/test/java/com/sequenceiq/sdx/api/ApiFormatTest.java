package com.sequenceiq.sdx.api;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.apiformat.ApiFormatValidator;
import com.sequenceiq.sdx.api.model.ModelDescriptions;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponseTest;
import com.sequenceiq.sdx.api.model.SdxClusterShapeTest;
import com.sequenceiq.sdx.api.model.diagnostics.docs.DiagnosticsOperationDescriptions;

public class ApiFormatTest {

    @Test
    public void testApiFormat() {
        ApiFormatValidator.builder()
                .modelPackage("com.sequenceiq.sdx.api.model")
                .excludedClasses(
                        SdxClusterDetailResponseTest.class,
                        SdxClusterDetailResponse.Builder.class,
                        ModelDescriptions.class,
                        DiagnosticsOperationDescriptions.class,
                        SdxClusterShapeTest.class,
                        ModelDescriptions.SdxRotateRdsCertificateDescription.class
                )
                .build()
                .validate();
    }
}
