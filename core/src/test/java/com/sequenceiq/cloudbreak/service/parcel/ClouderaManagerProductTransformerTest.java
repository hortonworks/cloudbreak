package com.sequenceiq.cloudbreak.service.parcel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
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

        Set<ClouderaManagerProduct> actual = underTest.transform(image, true);

        assertEquals(2, actual.size());
        assertTrue(assertCdhProduct(actual));
        verify(preWarmParcelParser).parseProductFromParcel(preWarmParcels, preWarmCsdList);
    }

    @Test
    public void testTransformShouldCreateASetOfClouderaManagerProductFromAnImageWhenPreWarmParcelsAreNotAvailable() {
        List<String> preWarmParcels = Collections.emptyList();
        List<String> preWarmCsdList = Collections.emptyList();
        Image image = createImage(preWarmParcels, preWarmCsdList);
        when(preWarmParcelParser.parseProductFromParcel(preWarmParcels, preWarmCsdList)).thenReturn(Optional.empty());

        Set<ClouderaManagerProduct> actual = underTest.transform(image, true);

        assertEquals(1, actual.size());
        assertTrue(assertCdhProduct(actual));
        verify(preWarmParcelParser).parseProductFromParcel(preWarmParcels, preWarmCsdList);
    }

    @Test
    public void testTransformShouldParseCDHFromAnImageWhenGetPrewarmParcelsIsFalse() {
        List<String> preWarmParcels = Collections.emptyList();
        List<String> preWarmCsdList = Collections.emptyList();
        Image image = createImage(preWarmParcels, preWarmCsdList);

        Set<ClouderaManagerProduct> actual = underTest.transform(image, false);

        assertEquals(1, actual.size());
        assertTrue(assertCdhProduct(actual));
        verify(preWarmParcelParser, never()).parseProductFromParcel(preWarmParcels, preWarmCsdList);
    }

    private boolean assertCdhProduct(Set<ClouderaManagerProduct> actual) {
        return actual.stream()
                .anyMatch(product -> "CDH".equals(product.getName())
                        && PARCEL_URL.equals(product.getParcel())
                        && PARCEL_VERSION.equals(product.getVersion()));
    }

    private Image createImage(List<String> preWarmParcels, List<String> preWarmCsdList) {
        return new Image(null, null, null, null, null, null, null, null, new StackDetails(null, createStackRepoDetails(), null), OS_TYPE,
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