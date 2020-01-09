package com.sequenceiq.cloudbreak.service.parcel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedServices;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@ExtendWith(MockitoExtension.class)
class ParcelServiceTest {

    @Mock
    private CmTemplateGeneratorService clusterTemplateGeneratorService;

    @Mock
    private RestClientFactory restClientFactory;

    @InjectMocks
    private ParcelService underTest;

    @Test
    void testFilterParcelsByBlueprintWhenNoParcelForServiceName() {
        Client client = mock(Client.class);
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder request = mock(Invocation.Builder.class);
        Response response = mock(Response.class);

        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("bpText");

        SupportedServices supportedServices = getSupportedServices("serv1");

        when(clusterTemplateGeneratorService.getServicesByBlueprint("bpText")).thenReturn(supportedServices);
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        when(client.target("http://parcel1.com/manifest.json")).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(request);
        when(request.get()).thenReturn(response);
        when(response.getStatusInfo()).thenReturn(Response.Status.OK);
        when(response.readEntity(String.class)).thenReturn(getManifestJson("otherService1"));

        Set<ClouderaManagerProduct> actual = underTest.filterParcelsByBlueprint(getParcels("http://parcel1.com/"), blueprint);

        Assertions.assertEquals(0, actual.size());
    }

    @Test
    void testFilterParcelsByBlueprintWhenAllParcelsNeed() {
        Client client = mock(Client.class);
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder request = mock(Invocation.Builder.class);
        Response response = mock(Response.class);

        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("bpText");

        SupportedServices supportedServices = getSupportedServices("serv1");

        when(clusterTemplateGeneratorService.getServicesByBlueprint("bpText")).thenReturn(supportedServices);
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        when(client.target("http://parcel1.com/manifest.json")).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(request);
        when(request.get()).thenReturn(response);
        when(response.getStatusInfo()).thenReturn(Response.Status.OK);
        when(response.readEntity(String.class)).thenReturn(getManifestJson("serv1"));

        List<ClouderaManagerProduct> parcels = getParcels("http://parcel1.com/");
        Set<ClouderaManagerProduct> actual = underTest.filterParcelsByBlueprint(parcels, blueprint);

        Assertions.assertEquals(1, actual.size());
        Assertions.assertEquals(actual, new HashSet<>(parcels));
    }

    @Test
    void testFilterParcelsByBlueprintWhenNoManifest() {
        Client client = mock(Client.class);
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder request = mock(Invocation.Builder.class);
        Response response = mock(Response.class);

        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("bpText");

        SupportedServices supportedServices = getSupportedServices("serv1");

        when(clusterTemplateGeneratorService.getServicesByBlueprint("bpText")).thenReturn(supportedServices);
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        when(client.target("http://parcel1.com/manifest.json")).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(request);
        when(request.get()).thenReturn(response);
        when(response.getStatusInfo()).thenReturn(Response.Status.OK);
        when(response.readEntity(String.class)).thenReturn(null);

        List<ClouderaManagerProduct> parcels = getParcels("http://parcel1.com/", "http://parcel2.com/");
        Set<ClouderaManagerProduct> actual = underTest.filterParcelsByBlueprint(parcels, blueprint);

        Assertions.assertEquals(2, actual.size());
        Assertions.assertEquals(actual, new HashSet<>(parcels));
    }

    private List<ClouderaManagerProduct> getParcels(String... parcelUrls) {
        return Arrays.stream(parcelUrls).map(s -> {
            ClouderaManagerProduct product = new ClouderaManagerProduct();
            product.setParcel(s);
            return product;
        }).collect(Collectors.toList());
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

    private String getManifestJson(String... componentNames) {
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
