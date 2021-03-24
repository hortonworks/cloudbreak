package com.sequenceiq.cloudbreak.service.parcel;

import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.PaywallCredentialPopulator;
import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedServices;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@ExtendWith(MockitoExtension.class)
public class ParcelServiceTest {

    @Mock
    private CmTemplateGeneratorService clusterTemplateGeneratorService;

    @Mock
    private RestClientFactory restClientFactory;

    @Mock
    private PaywallCredentialPopulator paywallCredentialPopulator;

    @InjectMocks
    private ParcelService underTest;

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Trying to download the parcel manifest in case of BASE image, when we can reach the file which is a standard " +
                        "manifest file and it DOES NOT contains the component then DO NOT assign the parcel.",
                        "http://parcel1.com/", OK, "NIFI", Optional.of(getManifestJson("otherService1")), true, 0},

                {"Trying to download the parcel manifest in case of BASE image, when we can reach the file which is a standard " +
                        "manifest file and it DOES contains the component then assign the parcel. (NIFI usecase)",
                        "http://parcel1.com/", OK, "NIFI", Optional.of(getManifestJson("NIFI")), true, 1},

                {"Trying to download the parcel manifest in case of BASE image, when we can reach the file which is a standard " +
                        "manifest file then it should assign the parcel, because we dont know that it is required, or not. (PROFILER usecase)",
                        "http://parcel1.com/", OK, "NIFI", Optional.of("{..ewwer"), true, 1},

                {"Trying to download the parcel manifest in case of BASE image, when we can NOT reach the manifest file " +
                        "then it should assign the parcel, because we can't check that it is required, or not. (authentication required usecase)",
                        "http://parcel1.com/", NOT_FOUND, "NIFI", Optional.empty(), true, 1},

                {"Trying to download the parcel manifest in case of PREWARMED image, when we can reach the file which is a standard " +
                        "manifest file and it DOES NOT contains the component then DO NOT assign the parcel.",
                        "http://parcel1.com/", OK, "NIFI", Optional.of(getManifestJson("otherService1")), false, 0},

                {"Trying to download the parcel manifest in case of PREWARMED image when we can reach the file which is a standard " +
                        "manifest file and it DOES contains the component then assign the parcel. (NIFI usecase)",
                        "http://parcel1.com/", OK, "NIFI", Optional.of(getManifestJson("NIFI")), false, 1},

                {"Trying to download the parcel manifest in case of PREWARMED image, when we can reach the file which is a standard " +
                        "manifest file then it should assign the parcel, because we dont know that it is required, or not. (PROFILER usecase)",
                        "http://parcel1.com/", OK, "NIFI", Optional.of("{..ewwer"), false, 1},

                {"Trying to download the parcel manifest in case of PREWARMED image, when we can NOT reach the manifest file " +
                        "then it should assign the parcel because probably that is not available anymore but the PREWARMED image contains it. " +
                        "(RELENG deleted a released artifact usecase)",
                        "http://parcel1.com/", NOT_FOUND, "NIFI", Optional.empty(), false, 1},
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void test(String description, String parcelUrl, Response.Status status, String serviceNameInTheBlueprint, Optional<String> manifest,
        boolean baseImage, int parcelCount) {
        when(clusterTemplateGeneratorService.getServicesByBlueprint("bpText"))
                .thenReturn(getSupportedServices(serviceNameInTheBlueprint));

        mockManifestResponse(parcelUrl, status, manifest);

        assertEquals(parcelCount, underTest.filterParcelsByBlueprint(getParcels(parcelUrl), getBlueprint(), baseImage).size());
    }

    public Blueprint getBlueprint() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("bpText");
        return blueprint;
    }

    public void mockManifestResponse(String parcelUrl, Response.Status status, Optional<String> manifest) {
        Client client = mock(Client.class);
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder request = mock(Invocation.Builder.class);
        Response response = mock(Response.class);
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        when(client.target(parcelUrl + "manifest.json")).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(request);
        when(request.get()).thenReturn(response);
        when(response.getStatusInfo()).thenReturn(status);
        if (manifest.isPresent()) {
            when(response.readEntity(String.class)).thenReturn(manifest.get());
        }
    }

    private Set<ClouderaManagerProduct> getParcels(String... parcelUrls) {
        return Arrays.stream(parcelUrls).map(s -> {
            ClouderaManagerProduct product = new ClouderaManagerProduct();
            product.setParcel(s);
            return product;
        }).collect(Collectors.toSet());
    }

    private SupportedServices getSupportedServices(String... componentNames) {
        SupportedServices supportedServices = new SupportedServices();
        Set<SupportedService> services = Arrays.stream(componentNames).map(s -> {
            SupportedService supportedService = new SupportedService();
            supportedService.setComponentNameInParcel(s);
            return supportedService;
        }).collect(Collectors.toSet());
        supportedServices.setServices(services);
        return supportedServices;
    }

    public static String getManifestJson(String... componentNames) {
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
        return JsonUtil.writeValueAsStringSilentSafe(manifest);
    }
}
