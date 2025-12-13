package com.sequenceiq.cloudbreak.logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.TestUtil;

class MDCBuilderTest {

    @Test
    void buildSimpleContext() {
        MDCBuilder.cleanupMdc();
        MDCBuilder.buildMdcContextFromCrn(null);
        assertNull(MDC.get(LoggerContextKey.TENANT.toString()));
        assertNull(MDC.get(LoggerContextKey.USER_CRN.toString()));
    }

    @Test
    void buildSimpleContextWithNull() {
        MDCBuilder.cleanupMdc();
        MDCBuilder.buildMdcContext();
        assertNull(MDC.get(LoggerContextKey.USER_CRN.toString()));
        assertNull(MDC.get(LoggerContextKey.RESOURCE_ID.toString()));
        assertNull(MDC.get(LoggerContextKey.RESOURCE_TYPE.toString()));
        assertNull(MDC.get(LoggerContextKey.RESOURCE_NAME.toString()));
    }

    @Test
    void buildContextWithStack() {
        MDCBuilder.cleanupMdc();
        MDCBuilder.buildMdcContext(TestUtil.stack());
        assertEquals("STACK", MDC.get(LoggerContextKey.RESOURCE_TYPE.toString()));
        assertEquals("simplestack", MDC.get(LoggerContextKey.RESOURCE_NAME.toString()));
        assertEquals("envCrn", MDC.get(LoggerContextKey.ENVIRONMENT_CRN.toString()));
        assertEquals("testtenant", MDC.get(LoggerContextKey.TENANT.toString()));
    }

    @Test
    void buildContextWithCredential() {
        MDCBuilder.cleanupMdc();
        MDCBuilder.buildMdcContext(TestUtil.awsCredential());
        assertEquals("CREDENTIAL", MDC.get(LoggerContextKey.RESOURCE_TYPE.toString()));
        assertEquals("dummyName", MDC.get(LoggerContextKey.RESOURCE_NAME.toString()));
        assertEquals("credCrn", MDC.get(LoggerContextKey.RESOURCE_CRN.toString()));
        assertNull(MDC.get(LoggerContextKey.TENANT.toString()));
    }

    @Test
    void buildMDCContext() {
        MDCBuilder.buildMdc(MdcContext.builder()
                .tenant("tenant")
                .environmentCrn("envCrn")
                .resourceName("resName")
                .resourceType("resType")
                .resourceCrn("resCrn")
                .requestId("reqId")
                .userCrn("userCrn")
                .flowId("flowId")
                .build());

        assertEquals("resType", MDC.get(LoggerContextKey.RESOURCE_TYPE.toString()));
        assertEquals("resName", MDC.get(LoggerContextKey.RESOURCE_NAME.toString()));
        assertEquals("resCrn", MDC.get(LoggerContextKey.RESOURCE_CRN.toString()));
        assertEquals("tenant", MDC.get(LoggerContextKey.TENANT.toString()));
        assertEquals("envCrn", MDC.get(LoggerContextKey.ENVIRONMENT_CRN.toString()));
        assertEquals("reqId", MDC.get(LoggerContextKey.REQUEST_ID.toString()));
        assertEquals("userCrn", MDC.get(LoggerContextKey.USER_CRN.toString()));
        assertEquals("flowId", MDC.get(LoggerContextKey.FLOW_ID.toString()));
    }
}