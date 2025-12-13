package com.sequenceiq.cloudbreak.logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class LogContextServiceTest {

    private static final String CLUSTER_NAME = "cluster1";

    private static final String CLUSTER_CRN = "clusterCrn1";

    private static final String ENVIRONMENT_CRN = "envCrn1";

    private static final String INITIATOR_USER_CRN = "user1";

    private static final String RESOURCE_TYPE = "TEST";

    @InjectMocks
    private LogContextService underTest;

    @BeforeEach
    public void setUp() {
        MDC.clear();
    }

    @AfterEach
    public void cleanUpEach() {
        MDC.clear();
    }

    @Test
    void testBuildMDCParamsWithCrnParamInRequest() {
        underTest.buildMDCParams(new TestController(), new String[] { "clusterName", "request", "environmentCrn" },
                new Object[] { CLUSTER_NAME, new TestRequest(CLUSTER_CRN), ENVIRONMENT_CRN });
        assertEquals(CLUSTER_CRN, MDC.get(LoggerContextKey.RESOURCE_CRN.toString()));
        assertEquals(CLUSTER_NAME, MDC.get(LoggerContextKey.RESOURCE_NAME.toString()));
        assertEquals(ENVIRONMENT_CRN, MDC.get(LoggerContextKey.ENVIRONMENT_CRN.toString()));
        assertEquals(RESOURCE_TYPE, MDC.get(LoggerContextKey.RESOURCE_TYPE.toString()));
    }

    @Test
    void testBuildMDCParamsWithCrnMethodParam() {
        underTest.buildMDCParams(new TestController(), new String[] { "clusterName", "clusterCrn", "environmentCrn" },
                new Object[] { CLUSTER_NAME, CLUSTER_CRN, ENVIRONMENT_CRN });
        assertEquals(CLUSTER_CRN, MDC.get(LoggerContextKey.RESOURCE_CRN.toString()));
        assertEquals(CLUSTER_NAME, MDC.get(LoggerContextKey.RESOURCE_NAME.toString()));
        assertEquals(ENVIRONMENT_CRN, MDC.get(LoggerContextKey.ENVIRONMENT_CRN.toString()));
        assertEquals(RESOURCE_TYPE, MDC.get(LoggerContextKey.RESOURCE_TYPE.toString()));
    }

    @Test
    void testBuildMDCParamsWithCrnAndInitatorUserCrnMethodParam() {
        underTest.buildMDCParams(new TestController(), new String[] { "clusterName", "clusterCrn", "environmentCrn", "initiatorUserCrn" },
                new Object[] { CLUSTER_NAME, CLUSTER_CRN, ENVIRONMENT_CRN, INITIATOR_USER_CRN });
        assertEquals(CLUSTER_CRN, MDC.get(LoggerContextKey.RESOURCE_CRN.toString()));
        assertEquals(CLUSTER_NAME, MDC.get(LoggerContextKey.RESOURCE_NAME.toString()));
        assertEquals(ENVIRONMENT_CRN, MDC.get(LoggerContextKey.ENVIRONMENT_CRN.toString()));
        assertEquals(RESOURCE_TYPE, MDC.get(LoggerContextKey.RESOURCE_TYPE.toString()));
    }

    private static final class TestRequest {

        private final String crn;

        private TestRequest(String crn) {
            this.crn = crn;
        }
    }

    private static final class TestController {

    }

}