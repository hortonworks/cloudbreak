package com.sequenceiq.cloudbreak.service.parcel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.service.image.PreWarmParcelParser;

@RunWith(MockitoJUnitRunner.class)
public class ClouderaManagerProductTransformerTest {

    private static final String PARCEL_URL = "http://parcel";

    private static final String PARCEL_VERSION = "7.1.0";

    private static final String PARCEL_NAME = "CDH-7.1.0";

    private static final String OS_TYPE = "redhat7";

    @InjectMocks
    private ClouderaManagerProductTransformer underTest;

    @Mock
    private PreWarmParcelParser preWarmParcelParser;

    @Test
    public void testTransformShouldCreateASetOfClouderaManagerProductFromAnImage() {
        List<String> preWarmParcels = Collections.emptyList();
        List<String> preWarmCsdList = Collections.emptyList();
        Image image = createImage(preWarmParcels, preWarmCsdList);
        when(preWarmParcelParser.parseProductFromParcel(preWarmParcels, preWarmCsdList)).thenReturn(Optional.of(new ClouderaManagerProduct()));

        Set<ClouderaManagerProduct> foundProducts = underTest.transform(image, true, true);

        assertThat(foundProducts, hasSize(2));
        assertTrue(assertCdhProduct(foundProducts));
        verify(preWarmParcelParser).parseProductFromParcel(preWarmParcels, preWarmCsdList);
    }

    @Test
    public void testTransformShouldCreateASetOfClouderaManagerProductFromAnImageWhenPreWarmParcelsAreNotAvailable() {
        List<String> preWarmParcels = Collections.emptyList();
        List<String> preWarmCsdList = Collections.emptyList();
        Image image = createImage(preWarmParcels, preWarmCsdList);
        when(preWarmParcelParser.parseProductFromParcel(preWarmParcels, preWarmCsdList)).thenReturn(Optional.empty());

        Set<ClouderaManagerProduct> foundProducts = underTest.transform(image, true, true);

        assertThat(foundProducts, hasSize(1));
        assertTrue(assertCdhProduct(foundProducts));
        verify(preWarmParcelParser).parseProductFromParcel(preWarmParcels, preWarmCsdList);
    }

    @Test
    public void testTransformShouldParseCDHFromAnImageWhenGetPrewarmParcelsIsFalse() {
        List<String> preWarmParcels = Collections.emptyList();
        List<String> preWarmCsdList = Collections.emptyList();
        Image image = createImage(preWarmParcels, preWarmCsdList);

        Set<ClouderaManagerProduct> foundProducts = underTest.transform(image, true, false);

        assertThat(foundProducts, hasSize(1));
        assertTrue(assertCdhProduct(foundProducts));
        verify(preWarmParcelParser, never()).parseProductFromParcel(any(), any());
    }

    @Test
    public void testTransformShouldParsePreWarmParcelsFromAnImageWhenGetCDHParcelsIsFalse() {
        List<String> preWarmParcels = Collections.emptyList();
        List<String> preWarmCsdList = Collections.emptyList();
        Image image = createImage(preWarmParcels, preWarmCsdList);
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
        Image image = createImage(preWarmParcels, preWarmCsdList);

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

    private Image createImage(List<String> preWarmParcels, List<String> preWarmCsdList) {
        return new Image(null, null, null, null, null, null, null, null, null, new ImageStackDetails(null, createStackRepoDetails(), null), OS_TYPE,
                null, List.of(preWarmParcels), preWarmCsdList, null, false, null, null);
    }

    private StackRepoDetails createStackRepoDetails() {
        Map<String, String> stackMap = new HashMap<>();
        stackMap.put(OS_TYPE, PARCEL_URL);
        stackMap.put("repository-version", PARCEL_VERSION);
        stackMap.put("repoid", PARCEL_NAME);
        return new StackRepoDetails(stackMap, null);
    }

}