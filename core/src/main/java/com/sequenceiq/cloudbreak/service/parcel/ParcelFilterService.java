package com.sequenceiq.cloudbreak.service.parcel;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.PaywallCredentialPopulator;
import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedService;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Service
public class ParcelFilterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcelFilterService.class);

    @Inject
    private CmTemplateGeneratorService clusterTemplateGeneratorService;

    @Inject
    private PaywallCredentialPopulator paywallCredentialPopulator;

    @Inject
    private RestClientFactory restClientFactory;

    public Set<ClouderaManagerProduct> filterParcelsByBlueprint(Set<ClouderaManagerProduct> parcels, Blueprint blueprint) {
        Set<String> serviceNamesInBlueprint = getAllServiceNameInBlueprint(blueprint);
        LOGGER.debug("The following services are found in the blueprint: {}", serviceNamesInBlueprint);
        Set<ClouderaManagerProduct> ret = new HashSet<>();
        if (serviceNamesInBlueprint.contains(null)) {
            LOGGER.debug("We can not identify one of the service from the blueprint so to stay on the safe side we will add every parcel");
            return parcels;
        }
        filterParcels(parcels, serviceNamesInBlueprint, ret);
        LOGGER.debug("The following parcels are used in CM based on blueprint: {}", ret);
        return ret;
    }

    private void filterParcels(Set<ClouderaManagerProduct> parcels, Set<String> serviceNamesInBlueprint, Set<ClouderaManagerProduct> ret) {
        parcels.forEach(parcel -> {
            ImmutablePair<ManifestStatus, Manifest> manifest = readRepoManifest(parcel.getParcel());
            if (manifest.right != null && ManifestStatus.SUCCESS.equals(manifest.left)) {
                Set<String> componentNamesInParcel = getAllComponentNameInParcel(manifest.right);
                LOGGER.debug("The following components are available in parcel: {}", componentNamesInParcel);
                if (componentNamesInParcel.stream().anyMatch(serviceNamesInBlueprint::contains)) {
                    LOGGER.info("Add parcel '{}' as there is at least one service both in the manifest and in the blueprint.", parcel);
                    ret.add(parcel);
                } else {
                    LOGGER.info("Skip parcel '{}' as there isn't any service both in the manifest and in the blueprint.", parcel);
                }
            } else {
                LOGGER.info("Add parcel '{}' as we were unable to check parcel's manifest.", parcel);
                ret.add(parcel);
            }
        });
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
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    private ImmutablePair<ManifestStatus, Manifest> readRepoManifest(String baseUrl) {
        String content = null;
        try {
            Client client = restClientFactory.getOrCreateDefault();
            WebTarget target = client.target(StringUtils.stripEnd(baseUrl, "/") + "/manifest.json");
            addPaywallCredentialsIfNecessary(baseUrl, target);
            Response response = target.request().get();
            content = readResponse(target, response);
            return ImmutablePair.of(ManifestStatus.SUCCESS, JsonUtil.readValue(content, Manifest.class));
        } catch (IOException e) {
            LOGGER.info("Could not parse manifest.json: {}, message: {}", content, e.getMessage());
            return ImmutablePair.of(ManifestStatus.COULD_NOT_PARSE, null);
        } catch (Exception e) {
            LOGGER.info("Could not read manifest.json from parcel repo: {}, message: {}", baseUrl, e.getMessage());
            return ImmutablePair.of(ManifestStatus.FAILED, null);
        }
    }

    private void addPaywallCredentialsIfNecessary(String baseUrl, WebTarget target) {
        paywallCredentialPopulator.populateWebTarget(baseUrl, target);
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
