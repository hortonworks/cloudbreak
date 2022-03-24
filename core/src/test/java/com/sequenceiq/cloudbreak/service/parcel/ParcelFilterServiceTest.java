package com.sequenceiq.cloudbreak.service.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedServices;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.ImageReaderService;

@ExtendWith(MockitoExtension.class)
public class ParcelFilterServiceTest {

    private static final long STACK_ID = 1L;

    private static final String BLUEPRINT_TEXT = "bpText";

    @Mock
    private CmTemplateGeneratorService clusterTemplateGeneratorService;

    @Mock
    private ImageReaderService imageReaderService;

    @Mock
    private ManifestRetrieverService manifestRetrieverService;

    @InjectMocks
    private ParcelFilterService underTest;

    private static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "Trying to download the parcel manifest in case of BASE image, when we can reach the file which is a standard " +
                        "manifest file and it DOES contains the component then assign the parcel. (NIFI usecase)",
                        "http://parcel1.com/", ManifestStatus.SUCCESS, "NIFI", "NIFI", 1 },

                { "Trying to download the parcel manifest in case of BASE image, when we can reach the file which is a standard " +
                        "manifest file then it should assign the parcel, because we dont know that it is required, or not. (PROFILER usecase)",
                        "http://parcel1.com/", ManifestStatus.SUCCESS, "NIFI", "{..ewwer", 1 },

                { "Trying to download the parcel manifest in case of BASE image, when we can NOT reach the manifest file " +
                        "then it should assign the parcel, because we can't check that it is required, or not. (authentication required usecase)",
                        "http://parcel1.com/", ManifestStatus.FAILED, "NIFI", null, 1 },

                { "Trying to download the parcel manifest in case of PREWARMED image when we can reach the file which is a standard " +
                        "manifest file and it DOES contains the component then assign the parcel. (NIFI usecase)",
                        "http://parcel1.com/", ManifestStatus.SUCCESS, "NIFI", "NIFI", 1 },

                { "Trying to download the parcel manifest in case of PREWARMED image, when we can reach the file which is a standard " +
                        "manifest file then it should assign the parcel, because we dont know that it is required, or not. (PROFILER usecase)",
                        "http://parcel1.com/", ManifestStatus.SUCCESS, "NIFI", "{..ewwer", 1 },

                { "Trying to download the parcel manifest in case of PREWARMED image, when we can NOT reach the manifest file " +
                        "then it should assign the parcel because probably that is not available anymore but the PREWARMED image contains it. " +
                        "(RELENG deleted a released artifact usecase)",
                        "http://parcel1.com/", ManifestStatus.FAILED, "NIFI", null, 1 },
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void test(String description, String parcelUrl, ManifestStatus status, String serviceNameInTheBlueprint, String manifest, int parcelCount) {
        when(clusterTemplateGeneratorService.getServicesByBlueprint(BLUEPRINT_TEXT)).thenReturn(getSupportedServices(Set.of(serviceNameInTheBlueprint)));
        when(manifestRetrieverService.readRepoManifest(parcelUrl)).thenReturn(ImmutablePair.of(status, getManifest(manifest)));

        assertEquals(parcelCount, underTest.filterParcelsByBlueprint(STACK_ID, getParcels(parcelUrl), getBlueprint()).size());
    }

    @Test
    void testShouldNotAddParcelWhenManifestDoesNotContainsTheComponentAndNotACustomParcel() {
        String parcelUrl = "http://parcel1.com/";
        String parcelName = "NIFI";
        ClouderaManagerProduct parcel = new ClouderaManagerProduct().withParcel(parcelUrl).withName(parcelName);
        when(clusterTemplateGeneratorService.getServicesByBlueprint(BLUEPRINT_TEXT)).thenReturn(getSupportedServices(Set.of(parcelName)));
        when(imageReaderService.getParcelNames(STACK_ID)).thenReturn(Set.of(parcelName));
        when(manifestRetrieverService.readRepoManifest(parcelUrl)).thenReturn(ImmutablePair.of(ManifestStatus.SUCCESS, getManifest("otherService1")));

        assertEquals(0, underTest.filterParcelsByBlueprint(STACK_ID, Set.of(parcel), getBlueprint()).size());
    }

    @Test
    void testShouldAddParcelWhenManifestDoesNotContainsTheComponentAndItIsACustomParcel() {
        String parcelUrl = "http://parcel1.com/";
        String parcelName = "CUSTOM";
        ClouderaManagerProduct parcel = new ClouderaManagerProduct().withParcel(parcelUrl).withName(parcelName);
        when(clusterTemplateGeneratorService.getServicesByBlueprint(BLUEPRINT_TEXT)).thenReturn(getSupportedServices(Set.of("NIFI")));
        when(imageReaderService.getParcelNames(STACK_ID)).thenReturn(Set.of("NIFI"));
        when(manifestRetrieverService.readRepoManifest(parcelUrl)).thenReturn(ImmutablePair.of(ManifestStatus.SUCCESS, getManifest(parcelName)));

        assertEquals(1, underTest.filterParcelsByBlueprint(STACK_ID, Set.of(parcel), getBlueprint()).size());
    }

    @Test
    void testShouldReturnAllParcelsWhenTheServiceNamesInTheBlueprintContainsANullValue() {
        String parcelUrl = "http://parcel1.com/";
        String parcelName = "CUSTOM";
        ClouderaManagerProduct parcel = new ClouderaManagerProduct().withParcel(parcelUrl).withName(parcelName);
        SupportedService supportedService = new SupportedService();
        supportedService.setComponentNameInParcel(null);
        SupportedServices supportedServices = new SupportedServices();
        supportedServices.setServices(Set.of(supportedService));
        when(clusterTemplateGeneratorService.getServicesByBlueprint(BLUEPRINT_TEXT)).thenReturn(supportedServices);

        assertEquals(1, underTest.filterParcelsByBlueprint(STACK_ID, Set.of(parcel), getBlueprint()).size());
    }

    @Test
    void testShouldAddOnlyCdhParcelWhenTheRequiredServicesAreAvailableInTheCdhParcelAndOtherParcelsAreNotAccessible() {
        when(clusterTemplateGeneratorService.getServicesByBlueprint(BLUEPRINT_TEXT)).thenReturn(getSupportedServices(Set.of("hdfs", "hive")));
        ClouderaManagerProduct cdhParcel = new ClouderaManagerProduct().withParcel("cdh-parcel-url").withName("CDH");
        ClouderaManagerProduct nifiParcel = new ClouderaManagerProduct().withParcel("nifi-parcel-url").withName("NIFI");
        when(manifestRetrieverService.readRepoManifest(cdhParcel.getParcel())).thenReturn(ImmutablePair.of(ManifestStatus.SUCCESS, getManifest("hdfs", "hive")));

        Set<ClouderaManagerProduct> actual = underTest.filterParcelsByBlueprint(STACK_ID, createParcelSet(cdhParcel, nifiParcel), getBlueprint());
        assertEquals(1, actual.size());
        assertTrue(actual.contains(cdhParcel));
    }

    @Test
    void testShouldAddCdhAndNifiParcelWhenTheRequiredServicesAreNotAvailableInTheCdhParcelAndOtherParcelsAreNotAccessible() {
        when(clusterTemplateGeneratorService.getServicesByBlueprint(BLUEPRINT_TEXT)).thenReturn(getSupportedServices(Set.of("hdfs", "hive", "spark")));
        ClouderaManagerProduct cdhParcel = new ClouderaManagerProduct().withParcel("cdh-parcel-url").withName("CDH");
        ClouderaManagerProduct nifiParcel = new ClouderaManagerProduct().withParcel("nifi-parcel-url").withName("NIFI");
        when(manifestRetrieverService.readRepoManifest(cdhParcel.getParcel())).thenReturn(ImmutablePair.of(ManifestStatus.SUCCESS, getManifest("hdfs", "hive")));
        when(manifestRetrieverService.readRepoManifest(nifiParcel.getParcel())).thenReturn(ImmutablePair.of(ManifestStatus.FAILED, null));

        Set<ClouderaManagerProduct> actual = underTest.filterParcelsByBlueprint(STACK_ID, createParcelSet(cdhParcel, nifiParcel), getBlueprint());
        assertEquals(2, actual.size());
        assertTrue(actual.contains(cdhParcel));
        assertTrue(actual.contains(nifiParcel));
    }

    private Blueprint getBlueprint() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("bpText");
        return blueprint;
    }

    private Set<ClouderaManagerProduct> getParcels(String... parcelUrls) {
        return Arrays.stream(parcelUrls).map(s -> {
            ClouderaManagerProduct product = new ClouderaManagerProduct();
            product.setParcel(s);
            return product;
        }).collect(Collectors.toSet());
    }

    private Set<ClouderaManagerProduct> createParcelSet(ClouderaManagerProduct... parcels) {
        return new LinkedHashSet<>(Arrays.asList(parcels));
    }

    private SupportedServices getSupportedServices(Set<String> componentNames) {
        SupportedServices supportedServices = new SupportedServices();
        Set<SupportedService> services = componentNames.stream().map(s -> {
            SupportedService supportedService = new SupportedService();
            supportedService.setComponentNameInParcel(s);
            return supportedService;
        }).collect(Collectors.toSet());
        supportedServices.setServices(services);
        return supportedServices;
    }

    private static Manifest getManifest(String... componentNames) {
        Manifest manifest = new Manifest();
        manifest.setLastUpdated(System.currentTimeMillis());
        Parcel parcel = new Parcel();
        List<Component> components = Arrays.stream(componentNames).map(s -> {
            Component component = new Component();
            component.setName(s);
            return component;
        }).collect(Collectors.toList());
        parcel.setComponents(components);
        manifest.setParcels(List.of(parcel));
        return manifest;
    }

}