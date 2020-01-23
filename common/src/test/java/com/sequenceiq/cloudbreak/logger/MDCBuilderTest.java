package com.sequenceiq.cloudbreak.logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MDCBuilderTest {

    @Test
    void testBuildWithViewClass() {
        ClusterView clusterView = new ClusterView("crn");

        MDCBuilder.buildMdcContext(clusterView);

        String resourceCrn = MDC.get(LoggerContextKey.RESOURCE_CRN.toString());
        String resourceType = MDC.get(LoggerContextKey.RESOURCE_TYPE.toString());

        assertEquals("crn", resourceCrn);
        assertEquals("CLUSTER", resourceType);
    }

    private static class ClusterView {
        private final String resourceCrn;

        ClusterView(String resourceCrn) {
            this.resourceCrn = resourceCrn;
        }

        public String getResourceCrn() {
            return resourceCrn;
        }
    }
}