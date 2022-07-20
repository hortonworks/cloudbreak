package com.sequenceiq.cloudbreak.service.upgrade.validation;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM_BUILD_NUMBER;
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
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;

@RunWith(MockitoJUnitRunner.class)
public class ParcelUrlProviderTest {

    private static final String IMAGE_ID = "image-id";

    private static final String STACK_BASE_URL = "http://test";

    private static final String STACK_REPO_VERSION = "2.7.6";

    private static final String PRE_WARM_CSD = "http://spark3.jar";

    private static final String OS_TYPE = "redhat7";

    @Mock
    private ParcelService parcelService;

    @InjectMocks
    private ParcelUrlProvider underTest;

    @Test
    public void testGetRequiredParcelsFromImageWhenTheStackTypeIsWorkload() {
        Stack stack = createStack(StackType.WORKLOAD);
        Image image = createImage(createStackRepoDetails(STACK_BASE_URL, STACK_REPO_VERSION));

        when(parcelService.getComponentsByImage(stack, image)).thenReturn(createClusterComponents());

        Set<String> actual = underTest.getRequiredParcelsFromImage(image, stack);

        assertEquals(3, actual.size());
        assertTrue(actual.contains("http://testCDH-2.7.6-el7.parcel"));
        assertTrue(actual.contains("http://test/spark/SPARK3-el7.parcel"));
        assertTrue(actual.contains(PRE_WARM_CSD));
        verify(parcelService).getComponentsByImage(stack, image);
    }

    @Test
    public void testGetRequiredParcelsFromImageWhenTheStackTypeIsDataLake() {
        Stack stack = createStack(StackType.DATALAKE);
        Image image = createImage(createStackRepoDetails(STACK_BASE_URL, STACK_REPO_VERSION));

        Set<String> actual = underTest.getRequiredParcelsFromImage(image, stack);

        assertEquals(1, actual.size());
        assertTrue(actual.contains("http://testCDH-2.7.6-el7.parcel"));
        verifyNoInteractions(parcelService);
    }

    private Set<ClusterComponentView> createClusterComponents() {
        ClusterComponentView clusterComponent = new ClusterComponentView();
        clusterComponent.setComponentType(ComponentType.CDH_PRODUCT_DETAILS);
        clusterComponent.setName("SPARK3");
        return Collections.singleton(clusterComponent);
    }

    private Image createImage(StackRepoDetails stackRepoDetails) {
        return new Image(null, null, null, null, null, IMAGE_ID, null, Map.of(OS_TYPE, "http://cm/yum"), null,
                new ImageStackDetails(null, stackRepoDetails, null),
                OS_TYPE, Map.of(CM.getKey(), "7.2.4", CM_BUILD_NUMBER.getKey(), "14450219"), createPreWarmParcels(), createPreWarmCsdUrls(), null, false, null,
                null);
    }

    private List<String> createPreWarmCsdUrls() {
        return List.of(PRE_WARM_CSD);
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