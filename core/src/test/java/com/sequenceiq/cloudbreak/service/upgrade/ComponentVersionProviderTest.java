package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ParcelInfoResponse;

@ExtendWith(MockitoExtension.class)
class ComponentVersionProviderTest {

    private static final String CENTOS_7 = "centos7";

    private static final String OS_PATCH_LEVEL = "2021-08-13";

    @InjectMocks
    private ComponentVersionProvider underTest;

    @Test
    void testGetComponentVersions() {

        ImageComponentVersions componentVersions = underTest.getComponentVersions(createPackageVersions(), CENTOS_7, OS_PATCH_LEVEL);

        List<ParcelInfoResponse> parcelInfoResponseList = componentVersions.getParcelInfoResponseList();
        assertEquals(4, parcelInfoResponseList.size());
        assertIterableEquals(createParcelInfoList(), parcelInfoResponseList);
        assertEquals(CENTOS_7, componentVersions.getOs());
        assertEquals(OS_PATCH_LEVEL, componentVersions.getOsPatchLevel());
        assertEquals("7.2.10", componentVersions.getCdp());
        assertEquals("7.4.2", componentVersions.getCm());
        assertEquals("16151091", componentVersions.getCdpGBN());
        assertEquals("15633910", componentVersions.getCmGBN());
    }

    private ArrayList<ParcelInfoResponse> createParcelInfoList() {
        ArrayList<ParcelInfoResponse> parcelInfoResponses = new ArrayList<>();
        parcelInfoResponses.add(new ParcelInfoResponse("Spark 3", "3.1.7280.2-11", "13959573"));
        parcelInfoResponses.add(new ParcelInfoResponse("Cloudera Flow Management", "2.2.1.0-56", "13885769"));
        parcelInfoResponses.add(new ParcelInfoResponse("Profiler Scheduler + Manager", "2.0.10.0-118", "13995067"));
        parcelInfoResponses.add(new ParcelInfoResponse("Cloudera Streaming Analytics with Apache Flink", "1.3.0.1-3", "14951294"));

        return parcelInfoResponses;
    }

    private Map<String, String> createPackageVersions() {
        Map<String, String> packageVersions = new HashMap<>();
        packageVersions.put("cdh-build-number", "16151091");
        packageVersions.put("cdp-logging-agent", "0.2.11");
        packageVersions.put("cdp-telemetry", "0.4.11");
        packageVersions.put("cfm", "2.2.1.0-56");
        packageVersions.put("cfm_gbn", "13885769");
        packageVersions.put("cloudbreak_images", "468dcbf");
        packageVersions.put("cm", "7.4.2");
        packageVersions.put("cm-build-number", "15633910");
        packageVersions.put("composite_gbn", "16201020");
        packageVersions.put("csa", "1.3.0.1-3");
        packageVersions.put("csa_gbn", "14951294");
        packageVersions.put("profiler", "2.0.10.0-118");
        packageVersions.put("profiler_gbn", "13995067");
        packageVersions.put("salt", "3000.8");
        packageVersions.put("salt-bootstrap", "0.13.4-2020-09-30T15:03:43");
        packageVersions.put("spark3", "3.1.7280.2-11");
        packageVersions.put("spark3_gbn", "13959573");
        packageVersions.put("stack", "7.2.10");
        return packageVersions;
    }
}