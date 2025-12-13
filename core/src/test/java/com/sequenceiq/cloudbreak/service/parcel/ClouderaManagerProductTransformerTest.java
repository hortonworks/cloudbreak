package com.sequenceiq.cloudbreak.service.parcel;

import static com.sequenceiq.common.model.OsType.CENTOS7;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.service.image.PreWarmParcelParser;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerProductTransformerTest {

    private static final String PARCEL_URL = "http://parcel";

    private static final String PARCEL_VERSION = "7.1.0";

    private static final String PARCEL_NAME = "CDH-7.1.0";

    @InjectMocks
    private ClouderaManagerProductTransformer underTest;

    @Mock
    private PreWarmParcelParser preWarmParcelParser;

    @Test
    public void testTransformShouldCreateASetOfClouderaManagerProductFromAnImage() {
        List<String> preWarmParcels = Collections.emptyList();
        List<String> preWarmCsdList = Collections.emptyList();
        Image image = createImage(preWarmParcels, preWarmCsdList, CENTOS7);
        when(preWarmParcelParser.parseProductFromParcel(preWarmParcels, preWarmCsdList)).thenReturn(Optional.of(new ClouderaManagerProduct()));

        Set<ClouderaManagerProduct> foundProducts = underTest.transform(image, true, true);

        assertThat(foundProducts, hasSize(2));
        assertTrue(assertCdhProduct(foundProducts));
        verify(preWarmParcelParser).parseProductFromParcel(preWarmParcels, preWarmCsdList);
    }

    @ParameterizedTest()
    @EnumSource(OsType.class)
    public void testTransformShouldCreateASetOfClouderaManagerProductFromAnImageWithProperOsType(OsType osType) {
        List<String> preWarmParcels = Collections.emptyList();
        List<String> preWarmCsdList = Collections.emptyList();
        Image image = createImage(preWarmParcels, preWarmCsdList, osType);

        when(preWarmParcelParser.parseProductFromParcel(preWarmParcels, preWarmCsdList)).thenReturn(Optional.of(new ClouderaManagerProduct()));

        Set<ClouderaManagerProduct> foundProducts = underTest.transform(image, true, true);

        assertThat(foundProducts, hasSize(2));
        assertTrue(assertCdhProductWithOsType(foundProducts, osType));
        verify(preWarmParcelParser).parseProductFromParcel(preWarmParcels, preWarmCsdList);
    }

    @Test
    public void testTransformShouldReturnEmptySetWhenTheImageIsNotPrewarmed() {
        Set<ClouderaManagerProduct> foundProducts = underTest.transform(createBaseImage(CENTOS7), true, true);

        assertTrue(foundProducts.isEmpty());
        verifyNoInteractions(preWarmParcelParser);
    }

    @Test
    public void testTransformShouldCreateASetOfClouderaManagerProductFromAnImageWhenPrewarmedParcelsAreNotAvailable() {
        List<String> preWarmParcels = Collections.emptyList();
        List<String> preWarmCsdList = Collections.emptyList();
        Image image = createImage(preWarmParcels, preWarmCsdList, CENTOS7);
        when(preWarmParcelParser.parseProductFromParcel(preWarmParcels, preWarmCsdList)).thenReturn(Optional.empty());

        Set<ClouderaManagerProduct> foundProducts = underTest.transform(image, true, true);

        assertThat(foundProducts, hasSize(1));
        assertTrue(assertCdhProduct(foundProducts));
        verify(preWarmParcelParser).parseProductFromParcel(preWarmParcels, preWarmCsdList);
    }

    @Test
    public void testTransformShouldParseCDHFromAnImageWhenGetPrewarmedParcelsIsFalse() {
        List<String> preWarmParcels = Collections.emptyList();
        List<String> preWarmCsdList = Collections.emptyList();
        Image image = createImage(preWarmParcels, preWarmCsdList, CENTOS7);

        Set<ClouderaManagerProduct> foundProducts = underTest.transform(image, true, false);

        assertThat(foundProducts, hasSize(1));
        assertTrue(assertCdhProduct(foundProducts));
        verify(preWarmParcelParser, never()).parseProductFromParcel(any(), any());
    }

    @Test
    public void testTransformShouldParsePreWarmParcelsFromAnImageWhenGetCDHParcelsIsFalse() {
        List<String> preWarmParcels = Collections.emptyList();
        List<String> preWarmCsdList = Collections.emptyList();
        Image image = createImage(preWarmParcels, preWarmCsdList, CENTOS7);
        when(preWarmParcelParser.parseProductFromParcel(preWarmParcels, preWarmCsdList)).thenReturn(Optional.of(new ClouderaManagerProduct()));

        Set<ClouderaManagerProduct> foundProducts = underTest.transform(image, false, true);

        assertThat(foundProducts, hasSize(1));
        assertFalse(assertCdhProduct(foundProducts));
        verify(preWarmParcelParser).parseProductFromParcel(preWarmParcels, preWarmCsdList);
    }

    @Test
    public void testTransformShouldNotParseFromAnImageWhenCDHAndPreWarmBothFalse() {
        List<String> preWarmParcels = Collections.emptyList();
        List<String> preWarmCsdList = Collections.emptyList();
        Image image = createImage(preWarmParcels, preWarmCsdList, CENTOS7);

        Set<ClouderaManagerProduct> foundProducts = underTest.transform(image, false, false);

        assertThat(foundProducts, empty());
        verify(preWarmParcelParser, never()).parseProductFromParcel(any(), any());
    }

    private boolean assertCdhProduct(Set<ClouderaManagerProduct> actual) {
        return actual.stream()
                .anyMatch(product -> "CDH".equals(product.getName())
                        && PARCEL_URL.equals(product.getParcel())
                        && PARCEL_VERSION.equals(product.getVersion()));
    }

    private boolean assertCdhProductWithOsType(Set<ClouderaManagerProduct> actual, OsType osType) {
        return actual.stream()
                .anyMatch(product -> "CDH".equals(product.getName())
                        && PARCEL_URL.equals(product.getParcel())
                        && PARCEL_VERSION.equals(product.getVersion())
                        && product.getParcelFileUrl().endsWith(osType.getParcelPostfix() + ".parcel"));
    }

    private Image createImage(List<String> preWarmParcels, List<String> preWarmCsdList, OsType osType) {
        return Image.builder()
                .withStackDetails(new ImageStackDetails(null, createStackRepoDetails(osType), null))
                .withOsType(osType.getOsType())
                .withPreWarmParcels(List.of(preWarmParcels))
                .withPreWarmCsd(preWarmCsdList)
                .withAdvertised(false)
                .build();
    }

    private Image createBaseImage(OsType osType) {
        return Image.builder().withOsType(osType.getOsType()).build();
    }

    private StackRepoDetails createStackRepoDetails(OsType osType) {
        Map<String, String> stackMap = new HashMap<>();
        stackMap.put(osType.getOsType(), PARCEL_URL);
        stackMap.put("repository-version", PARCEL_VERSION);
        stackMap.put("repoid", PARCEL_NAME);
        return new StackRepoDetails(stackMap, null);
    }

}