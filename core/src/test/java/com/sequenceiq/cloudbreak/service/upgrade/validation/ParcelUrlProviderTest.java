package com.sequenceiq.cloudbreak.service.upgrade.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
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
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
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
    public void testGetRequiredParcelsFromImageWhenTheStackTypeIsWorkload() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = createStack(StackType.WORKLOAD);
        StatedImage image = createImage(createStackRepoDetails(STACK_BASE_URL, STACK_REPO_VERSION));

        when(imageCatalogService.getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID)).thenReturn(image);
        when(parcelService.getComponentsByImage(stack, image.getImage())).thenReturn(createClusterComponents());

        Set<String> actual = underTest.getRequiredParcelsFromImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID, stack);

        assertEquals(2, actual.size());
        assertTrue(actual.contains("http://testCDH-2.7.6-el7.parcel"));
        assertTrue(actual.contains("http://test/spark/SPARK3-el7.parcel"));
        verify(imageCatalogService).getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID);
        verify(parcelService).getComponentsByImage(stack, image.getImage());
    }

    @Test
    public void testGetRequiredParcelsFromImageWhenTheStackTypeIsDataLake() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = createStack(StackType.DATALAKE);
        StatedImage image = createImage(createStackRepoDetails(STACK_BASE_URL, STACK_REPO_VERSION));

        when(imageCatalogService.getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID)).thenReturn(image);

        Set<String> actual = underTest.getRequiredParcelsFromImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID, stack);

        assertEquals(1, actual.size());
        assertTrue(actual.contains("http://testCDH-2.7.6-el7.parcel"));
        verify(imageCatalogService).getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID);
        verifyNoInteractions(parcelService);
    }

    @Test(expected = CloudbreakServiceException.class)
    public void testGetRequiredParcelsFromImageShouldThrowExceptionWhenTheImageCatalogThrowsAnException()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = createStack(StackType.DATALAKE);

        when(imageCatalogService.getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID)).thenThrow(new CloudbreakImageNotFoundException("Error"));

        underTest.getRequiredParcelsFromImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID, stack);

        verify(imageCatalogService).getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID);
        verifyNoInteractions(parcelService);
    }

    @Test
    public void testGetRequiredParcelsFromImageShouldThrowExceptionThenTheStackRepoVersionIsNotAvailable()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = createStack(StackType.DATALAKE);
        StatedImage image = createImage(createStackRepoDetails(STACK_BASE_URL, null));

        when(imageCatalogService.getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID)).thenReturn(image);

        Exception exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.getRequiredParcelsFromImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID, stack));

        assertEquals("Stack repository version is not found on image: image-id", exception.getMessage());
        verify(imageCatalogService).getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID);
        verifyNoInteractions(parcelService);
    }

    @Test
    public void testGetRequiredParcelsFromImageShouldThrowExceptionThenTheStackBaseUrlIsNotAvailable()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = createStack(StackType.DATALAKE);
        StatedImage image = createImage(createStackRepoDetails(null, STACK_REPO_VERSION));

        when(imageCatalogService.getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID)).thenReturn(image);

        Exception exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.getRequiredParcelsFromImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID, stack));

        assertEquals("Stack base URL is not found on image: image-id", exception.getMessage());
        verify(imageCatalogService).getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID);
        verifyNoInteractions(parcelService);
    }

    private Set<ClusterComponent> createClusterComponents() {
        return Collections.singleton(new ClusterComponent(ComponentType.CDH_PRODUCT_DETAILS, "SPARK3", null, null));
    }

    private StatedImage createImage(StackRepoDetails stackRepoDetails) {
        return StatedImage.statedImage(new Image(null, null, null, null, IMAGE_ID, null, null, null, new StackDetails(null, stackRepoDetails, null), null,
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