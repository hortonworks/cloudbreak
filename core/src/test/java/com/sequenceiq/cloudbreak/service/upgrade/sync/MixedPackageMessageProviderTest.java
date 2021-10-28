package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.service.upgrade.sync.common.ParcelInfo;

public class MixedPackageMessageProviderTest {

    private static final String CDH_KEY = "CDH";

    private static final String V_7_2_2 = "7.2.2";

    private final MixedPackageMessageProvider underTest = new MixedPackageMessageProvider();

    @Test
    void testCreateActiveParcelsMessageShouldReturnAMessageFromParcelInfo() {
        String actual = underTest.createActiveParcelsMessage(createParcelInfo(Map.of(CDH_KEY, V_7_2_2)));
        assertEquals("CDH 7.2.2", actual);
    }

    @Test
    void testCreateActiveParcelsMessageShouldReturnEmptyStringWhenTheParcelInfoIsEmpty() {
        assertFalse(StringUtils.hasText(underTest.createActiveParcelsMessage(Collections.emptySet())));
    }

    @Test
    void testCreateMessageFromMapShouldReturnAMessageFromMap() {
        String actual = underTest.createMessageFromMap(Map.of(CDH_KEY, V_7_2_2));
        assertEquals("CDH 7.2.2", actual);
    }

    @Test
    void testCreateMessageFromMapShouldReturnEmptyStringWhenTheMapIsEmpty() {
        assertFalse(StringUtils.hasText(underTest.createMessageFromMap(Collections.emptyMap())));
    }

    @Test
    void testCreateSuggestedVersionsMessageShouldReturnMessage() {
        Map<String, String> productsFromImage = Map.of(CDH_KEY, V_7_2_2, "SPARK", V_7_2_2, "NIFI", V_7_2_2, "FLINK", V_7_2_2);
        Map<String, String> activeParcels = Map.of(CDH_KEY, V_7_2_2);

        String actual = underTest.createSuggestedVersionsMessage(productsFromImage, createParcelInfo(activeParcels), V_7_2_2);

        assertEquals("Cloudera Manager 7.2.2, CDH 7.2.2", actual);
    }

    private Set<ParcelInfo> createParcelInfo(Map<String, String> parcels) {
        return parcels.entrySet().stream().map(entry -> new ParcelInfo(entry.getKey(), entry.getValue())).collect(Collectors.toSet());
    }

}