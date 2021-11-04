package com.sequenceiq.cloudbreak.service.upgrade.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;

@RunWith(MockitoJUnitRunner.class)
public class ParcelUrlProviderTest {

    private static final String IMAGE_CATALOG_URL = "image-catalog-url";

    private static final String IMAGE_CATALOG_NAME = "image-catalog-name";

    private static final String IMAGE_ID = "image-id";

    private static final String STACK_BASE_URL = "http://test";

    private static final String STACK_REPO_VERSION = "2.7.6";

    @Mock
    private ParcelService parcelService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @InjectMocks
    private ParcelUrlProvider underTest;

    @Test
    public void testGetRequiredParcelsFromImageWhenTheStackTypeIsWorkload() {
        Stack stack = createStack(StackType.WORKLOAD);
        StatedImage image = createImage(createStackRepoDetails(STACK_BASE_URL, STACK_REPO_VERSION));

        when(parcelService.getComponentsByImage(stack, image.getImage())).thenReturn(createClusterComponents());

        Set<String> actual = underTest.getRequiredParcelsFromImage(image, stack);

        assertEquals(2, actual.size());
        assertTrue(actual.contains("http://testCDH-2.7.6-el7.parcel"));
        assertTrue(actual.contains("http://test/spark/SPARK3-el7.parcel"));
        verify(parcelService).getComponentsByImage(stack, image.getImage());
    }

    @Test
    public void testGetRequiredParcelsFromImageWhenTheStackTypeIsDataLake() {
        Stack stack = createStack(StackType.DATALAKE);
        StatedImage image = createImage(createStackRepoDetails(STACK_BASE_URL, STACK_REPO_VERSION));

        Set<String> actual = underTest.getRequiredParcelsFromImage(image, stack);

        assertEquals(1, actual.size());
        assertTrue(actual.contains("http://testCDH-2.7.6-el7.parcel"));
        verifyNoInteractions(parcelService);
    }

    private Set<ClusterComponent> createClusterComponents() {
        return Collections.singleton(new ClusterComponent(ComponentType.CDH_PRODUCT_DETAILS, "SPARK3", null, null));
    }

    private StatedImage createImage(StackRepoDetails stackRepoDetails) {
        return StatedImage.statedImage(new Image(null, null, null, null, IMAGE_ID, null, null, null, new ImageStackDetails(null, stackRepoDetails, null), null,
                null, createPreWarmParcels(), null, null, false, null, null), IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
    }

    private List<List<String>> createPreWarmParcels() {
        return List.of(
                List.of("SPARK3-el7.parcel", "http://test/spark/."),
                List.of("FLINK-el7.parcel", "http://test/flink"),
                List.of("PROFILER_MANAGER-el7.parcel", "http://test/."),
                List.of("CFM-el7.parcel", "http://test/cfm"));
    }

    private StackRepoDetails createStackRepoDetails(String baseUrl, String stackRepoVersion) {
        Map<String, String> stackMap = new HashMap<>();
        stackMap.put("redhat7", baseUrl);
        stackMap.put("repository-version", stackRepoVersion);
        return new StackRepoDetails(stackMap, null);
    }

    private Stack createStack(StackType stackType) {
        Stack stack = new Stack();
        stack.setType(stackType);
        return stack;
    }

}