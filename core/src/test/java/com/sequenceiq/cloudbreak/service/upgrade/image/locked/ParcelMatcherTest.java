package com.sequenceiq.cloudbreak.service.upgrade.image.locked;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.image.PreWarmParcelParser;

@ExtendWith(MockitoExtension.class)
class ParcelMatcherTest {

    private static final List<String> PREWARMED_PARCEL1 = List.of("PARCELNAMEWITHVERSION1", "PARCELURL2");

    private static final List<String> PREWARMED_PARCEL2 = List.of("PARCELNAMEWITHVERSION2", "PARCELURL2");

    @Mock
    private Image image;

    @Mock
    private PreWarmParcelParser preWarmParcelParser;

    @InjectMocks
    private ParcelMatcher underTest;

    @BeforeEach
    public void init() {
        lenient().when(image.getPreWarmParcels()).thenReturn(List.of(PREWARMED_PARCEL1, PREWARMED_PARCEL2));
        lenient().when(preWarmParcelParser.parseProductFromParcel(anyList(), eq(Collections.emptyList()))).thenReturn(Optional.empty());
    }

    @Test
    public void testPrewarmedAndActivatedMatching() {
        Map<String, String> activatedParcels = Map.of("pARCEL1NAME", "PARCEL1VERSION", "PARCEL2NAMe", "PARCEL2VERSION");
        when(preWarmParcelParser.parseProductFromParcel(PREWARMED_PARCEL1, Collections.emptyList()))
                .thenReturn(createClouderaManagerProduct("PARCEL1NAME", "PARCEL1VERSION"));
        when(preWarmParcelParser.parseProductFromParcel(PREWARMED_PARCEL2, Collections.emptyList()))
                .thenReturn(createClouderaManagerProduct("PARCEL2NAME", "PARCEL2VERSION"));

        boolean result = underTest.isMatchingNonCdhParcels(image, activatedParcels);

        assertTrue(result);
    }

    @Test
    public void testPrewarmedMissingParcel() {
        Map<String, String> activatedParcels = Map.of("PARCEL1NAME", "PARCEL1VERSION", "PARCEL2NAME", "PARCEL2VERSION");
        when(preWarmParcelParser.parseProductFromParcel(PREWARMED_PARCEL1, Collections.emptyList()))
                .thenReturn(createClouderaManagerProduct("PARCEL1NAME", "PARCEL1VERSION"));
        when(preWarmParcelParser.parseProductFromParcel(PREWARMED_PARCEL2, Collections.emptyList()))
                .thenReturn(Optional.empty());

        boolean result = underTest.isMatchingNonCdhParcels(image, activatedParcels);

        assertFalse(result);
    }

    @Test
    public void testPrewarmedAndActivatedHasDifferentVersion() {
        Map<String, String> activatedParcels = Map.of("PARCEL1NAME", "PARCEL1VERSION", "PARCEL2NAME", "PARCEL2VERSION");
        when(preWarmParcelParser.parseProductFromParcel(PREWARMED_PARCEL1, Collections.emptyList()))
                .thenReturn(createClouderaManagerProduct("PARCEL1NAME", "PARCEL1VERSION"));
        when(preWarmParcelParser.parseProductFromParcel(PREWARMED_PARCEL2, Collections.emptyList()))
                .thenReturn(createClouderaManagerProduct("PARCEL2NAME", "PARCEL2VERSIONDIFF"));

        boolean result = underTest.isMatchingNonCdhParcels(image, activatedParcels);

        assertFalse(result);
    }

    @Test
    public void testPrewarmedHasExtraParcel() {
        Map<String, String> activatedParcels = Map.of("pARCEL1NAME", "PARCEL1VERSION");
        when(preWarmParcelParser.parseProductFromParcel(PREWARMED_PARCEL1, Collections.emptyList()))
                .thenReturn(createClouderaManagerProduct("PARCEL1NAME", "PARCEL1VERSION"));
        when(preWarmParcelParser.parseProductFromParcel(PREWARMED_PARCEL2, Collections.emptyList()))
                .thenReturn(createClouderaManagerProduct("PARCEL2NAME", "PARCEL2VERSION"));

        boolean result = underTest.isMatchingNonCdhParcels(image, activatedParcels);

        assertTrue(result);
    }

    @Test
    public void testCdhActivatedParcelIgnored() {
        Map<String, String> activatedParcels = Map.of("PARCEL1NAME", "PARCEL1VERSION", "PARCEL2NAME", "PARCEL2VERSION", "CDH", "CDHVERSION");
        when(preWarmParcelParser.parseProductFromParcel(PREWARMED_PARCEL1, Collections.emptyList()))
                .thenReturn(createClouderaManagerProduct("PARCEL1NAME", "PARCEL1VERSION"));
        when(preWarmParcelParser.parseProductFromParcel(PREWARMED_PARCEL2, Collections.emptyList()))
                .thenReturn(createClouderaManagerProduct("PARCEL2NAME", "PARCEL2VERSION"));

        boolean result = underTest.isMatchingNonCdhParcels(image, activatedParcels);

        assertTrue(result);
    }

    @Test
    public void testCdhIsOnlyActivated() {
        Map<String, String> activatedParcels = Map.of("CDH", "CDHVERSION");

        boolean result = underTest.isMatchingNonCdhParcels(image, activatedParcels);

        assertTrue(result);
    }

    private Optional<ClouderaManagerProduct> createClouderaManagerProduct(String name, String version) {
        ClouderaManagerProduct product = new ClouderaManagerProduct().withName(name).withVersion(version);
        return Optional.of(product);
    }
}