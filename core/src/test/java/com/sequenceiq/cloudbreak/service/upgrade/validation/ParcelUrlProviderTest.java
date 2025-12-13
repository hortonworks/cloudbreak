package com.sequenceiq.cloudbreak.service.upgrade.validation;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM_BUILD_NUMBER;
import static com.sequenceiq.cloudbreak.cluster.model.ParcelStatus.ACTIVATED;
import static com.sequenceiq.cloudbreak.cluster.model.ParcelStatus.DISTRIBUTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.CmServerQueryService;

@ExtendWith(MockitoExtension.class)
class ParcelUrlProviderTest {

    private static final String IMAGE_ID = "image-id";

    private static final String STACK_BASE_URL = "http://test";

    private static final String STACK_REPO_VERSION = "2.7.6";

    private static final String PRE_WARM_CSD = "http://spark3.jar";

    private static final String OS_TYPE = "redhat7";

    @Mock
    private ParcelService parcelService;

    @Mock
    private CmServerQueryService cmServerQueryService;

    @Mock
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Mock
    private StackDto stackDto;

    @InjectMocks
    private ParcelUrlProvider underTest;

    @Test
    void testGetRequiredParcelsFromImageShouldReturnRequiredParcelUrlsWhenTheStackTypeIsWorkload() {
        Stack stack = createStack(StackType.WORKLOAD);
        Image image = createImage(createStackRepoDetails(STACK_BASE_URL, STACK_REPO_VERSION));
        ClouderaManagerProduct cdh = createCmProduct("CDH", "7.2.15", "https://cdh.parcel");
        ClouderaManagerProduct spark3 = createCmProduct("SPARK3", "1.2.4", "https://spark3.parcel").withCsd(Collections.singletonList(PRE_WARM_CSD));
        ClouderaManagerProduct nifi = createCmProduct("NIFI", "4.5.6", "https://nifi.parcel");
        when(stackDto.getStack()).thenReturn(stack);
        when(parcelService.getComponentNamesByImage(stackDto, image)).thenReturn(Set.of("CDH", "SPARK3"));
        when(cmServerQueryService.queryAllParcels(stackDto)).thenReturn(
                Set.of(new ParcelInfo("CDH", "7.2.7", ACTIVATED), new ParcelInfo("SPARK3", "1.2.3", ACTIVATED)));
        when(clouderaManagerProductTransformer.transform(image, true, true)).thenReturn(Set.of(cdh, spark3, nifi));

        Set<String> actual = underTest.getRequiredParcelsFromImage(image, stackDto);

        assertEquals(3, actual.size());
        assertTrue(actual.contains(cdh.getParcelFileUrl()));
        assertTrue(actual.contains(spark3.getParcelFileUrl()));
        assertTrue(actual.contains(PRE_WARM_CSD));
        verify(parcelService).getComponentNamesByImage(stackDto, image);
        verify(cmServerQueryService).queryAllParcels(stackDto);
        verify(clouderaManagerProductTransformer).transform(image, true, true);
    }

    @Test
    void testGetRequiredParcelsFromImageShouldReturnRequiredParcelUrlsWhenTheStackTypeIsDataLake() {
        Stack stack = createStack(StackType.DATALAKE);
        Image image = createImage(createStackRepoDetails(STACK_BASE_URL, STACK_REPO_VERSION));
        ClouderaManagerProduct cdh = createCmProduct("CDH", "7.2.15", "https://cdh.parcel");
        when(stackDto.getStack()).thenReturn(stack);
        when(parcelService.getComponentNamesByImage(stackDto, image)).thenReturn(Collections.singleton("CDH"));
        when(cmServerQueryService.queryAllParcels(stackDto)).thenReturn(Set.of(new ParcelInfo("CDH", "7.2.7", ACTIVATED)));
        when(clouderaManagerProductTransformer.transform(image, true, false)).thenReturn(Collections.singleton(cdh));

        Set<String> actual = underTest.getRequiredParcelsFromImage(image, stackDto);

        assertEquals(1, actual.size());
        assertTrue(actual.contains(cdh.getParcelFileUrl()));
        verify(parcelService).getComponentNamesByImage(stackDto, image);
        verify(cmServerQueryService).queryAllParcels(stackDto);
        verify(clouderaManagerProductTransformer).transform(image, true, false);
    }

    @Test
    void testGetRequiredParcelsFromImageShouldReturnEmptyListWhenAllRequiredParcelIsAlreadyDistributed() {
        Stack stack = createStack(StackType.WORKLOAD);
        Image image = createImage(createStackRepoDetails(STACK_BASE_URL, STACK_REPO_VERSION));
        ClouderaManagerProduct cdh = createCmProduct("CDH", "7.2.15", "https://cdh.parcel");
        ClouderaManagerProduct spark3 = createCmProduct("SPARK3", "1.2.4", "https://spark3.parcel").withCsd(Collections.singletonList(PRE_WARM_CSD));
        ClouderaManagerProduct nifi = createCmProduct("NIFI", "4.5.6", "https://nifi.parcel");
        when(stackDto.getStack()).thenReturn(stack);
        when(parcelService.getComponentNamesByImage(stackDto, image)).thenReturn(Set.of("CDH", "SPARK3"));
        when(cmServerQueryService.queryAllParcels(stackDto)).thenReturn(
                Set.of(new ParcelInfo(cdh.getName(), "7.2.7", ACTIVATED), new ParcelInfo(spark3.getName(), "1.2.3", ACTIVATED),
                        new ParcelInfo(cdh.getName(), cdh.getVersion(), DISTRIBUTED), new ParcelInfo(spark3.getName(), spark3.getVersion(), DISTRIBUTED)));
        when(clouderaManagerProductTransformer.transform(image, true, true)).thenReturn(Set.of(cdh, spark3, nifi));

        Set<String> actual = underTest.getRequiredParcelsFromImage(image, stackDto);

        assertTrue(actual.isEmpty());
        verify(parcelService).getComponentNamesByImage(stackDto, image);
        verify(cmServerQueryService).queryAllParcels(stackDto);
        verify(clouderaManagerProductTransformer).transform(image, true, true);
    }

    private Image createImage(StackRepoDetails stackRepoDetails) {
        return Image.builder()
                .withUuid(IMAGE_ID)
                .withRepo(Map.of(OS_TYPE, "http://cm/yum"))
                .withOsType(OS_TYPE)
                .withPackageVersions(Map.of(CM.getKey(), "7.2.4", CM_BUILD_NUMBER.getKey(), "14450219"))
                .withPreWarmParcels(createPreWarmParcels())
                .withPreWarmCsd(createPreWarmCsdUrls())
                .build();
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

    private ClouderaManagerProduct createCmProduct(String name, String version, String parcelFileUrl) {
        return new ClouderaManagerProduct().withName(name).withVersion(version).withParcelFileUrl(parcelFileUrl);
    }

}