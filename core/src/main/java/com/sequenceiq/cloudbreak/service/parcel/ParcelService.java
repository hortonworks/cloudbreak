package com.sequenceiq.cloudbreak.service.parcel;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedService;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Service
public class ParcelService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcelService.class);

    @Inject
    private CmTemplateGeneratorService clusterTemplateGeneratorService;

    @Inject
    private RestClientFactory restClientFactory;

    public Set<ClouderaManagerProduct> filterParcelsByBlueprint(List<ClouderaManagerProduct> parcels, Blueprint blueprint) {
        Set<String> serviceNamesInBlueprint = getAllServiceNameInBlueprint(blueprint);
        Set<ClouderaManagerProduct> ret = new HashSet<>();
        parcels.forEach(parcel -> {
            Manifest manifest = readRepoManifest(parcel.getParcel());
            if (manifest != null) {
                Set<String> componentNamesInParcel = getAllComponentNameInParcel(manifest);
                if (componentNamesInParcel.stream().anyMatch(serviceNamesInBlueprint::contains)) {
                    ret.add(parcel);
                }
            } else {
                ret.add(parcel);
            }
        });
        return ret;
    }

    private Set<String> getAllServiceNameInBlueprint(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintText();
        Set<SupportedService> supportedServices = clusterTemplateGeneratorService.getServicesByBlueprint(blueprintText).getServices();
        return supportedServices.stream()
                .map(SupportedService::getComponentNameInParcel)
                .collect(Collectors.toSet());
    }

    private Set<String> getAllComponentNameInParcel(Manifest manifest) {
        return manifest.getParcels().stream()
                .flatMap(it -> it.getComponents().stream())
                .map(Component::getName)
                .collect(Collectors.toSet());
    }

    private Manifest readRepoManifest(String baseUrl) {
        String content = null;
        try {
            Client client = restClientFactory.getOrCreateDefault();
            WebTarget target = client.target(StringUtils.stripEnd(baseUrl, "/") + "/manifest.json");
            Response response = target.request().get();
            content = readResponse(target, response);
            return JsonUtil.readValue(content, Manifest.class);
        } catch (IOException e) {
            LOGGER.info("Could not parse manifest.json: {}, message: {}", content, e.getMessage());
        } catch (Exception e) {
            LOGGER.info("Could not read manifest.json from parcel repo: {}, message: {}", baseUrl, e.getMessage());
        }
        return null;
    }

    private String readResponse(WebTarget target, Response response) {
        if (!response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
            throw new RuntimeException(String.format("Failed to get manifest.json from '%s' due to: '%s'",
                    target.getUri().toString(), response.getStatusInfo().getReasonPhrase()));
        }
        try {
            return response.readEntity(String.class);
        } catch (ProcessingException e) {
            throw new RuntimeException(String.format("Failed to process manifest.json from '%s' due to: '%s'",
                    target.getUri().toString(), e.getMessage()));
        }
    }
}
