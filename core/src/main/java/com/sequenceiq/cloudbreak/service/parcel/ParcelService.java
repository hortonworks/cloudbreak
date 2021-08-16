package com.sequenceiq.cloudbreak.service.parcel;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedService;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;

@Service
public class ParcelService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcelService.class);

    @Inject
    private CmTemplateGeneratorService clusterTemplateGeneratorService;

    @Inject
    private RestClientFactory restClientFactory;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private PaywallCredentialPopulator paywallCredentialPopulator;

    @Inject
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    public Set<ClusterComponent> getParcelComponentsByBlueprint(Stack stack) {
        Set<ClusterComponent> components = getComponents(stack);
        LOGGER.debug("The following components are available in the cluster {}", components);
        if (stack.isDatalake()) {
            return getDataLakeClusterComponents(components);
        } else {
            Map<String, ClusterComponent> cmProductMap = new HashMap<>();
            Set<ClouderaManagerProduct> cmProducts = new HashSet<>();
            for (ClusterComponent clusterComponent : components) {
                ClouderaManagerProduct product = clusterComponent.getAttributes().getSilent(ClouderaManagerProduct.class);
                cmProductMap.put(product.getName(), clusterComponent);
                cmProducts.add(product);
            }
            cmProducts = filterParcelsByBlueprint(cmProducts, stack.getCluster().getBlueprint(), false);
            Set<ClusterComponent> componentsByRequiredProducts = getComponentsByRequiredProducts(cmProductMap, cmProducts);
            LOGGER.debug("The following components are required for cluster {}", componentsByRequiredProducts);
            return componentsByRequiredProducts;
        }
    }

    public Set<ClusterComponent> getComponentsByImage(Stack stack, Image image) {
        Set<ClusterComponent> components = getComponents(stack);
        if (stack.isDatalake()) {
            return getDataLakeClusterComponents(components);
        } else {
            Map<String, ClusterComponent> cmProductMap = collectClusterComponentsByName(components);
            Set<ClouderaManagerProduct> cmProducts = clouderaManagerProductTransformer
                    .transform(image, true, true);
            cmProducts = filterParcelsByBlueprint(cmProducts, stack.getCluster().getBlueprint(), false);
            LOGGER.debug("The following parcels are used in CM based on blueprint: {}", cmProducts);
            return getComponentsByRequiredProducts(cmProductMap, cmProducts);
        }
    }

    private Map<String, ClusterComponent> collectClusterComponentsByName(Set<ClusterComponent> components) {
        return components.stream().collect(Collectors.toMap(ClusterComponent::getName, component -> component));
    }

    private Set<ClusterComponent> getComponents(Stack stack) {
        return clusterComponentConfigProvider.getComponentsByClusterId(stack.getCluster().getId()).stream()
                .filter(clusterComponent -> ComponentType.CDH_PRODUCT_DETAILS == clusterComponent.getComponentType())
                .collect(Collectors.toSet());
    }

    private Set<ClusterComponent> getDataLakeClusterComponents(Set<ClusterComponent> components) {
        ClusterComponent stackComponent = getCdhComponent(components);
        LOGGER.debug("For datalake clusters only the CDH parcel is used in CM: {}", stackComponent);
        return Collections.singleton(stackComponent);
    }

    private ClusterComponent getCdhComponent(Set<ClusterComponent> components) {
        return components.stream()
                .filter(clusterComponent -> clusterComponent.getName().equals(StackType.CDH.name()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Runtime component not found!"));
    }

    public Set<ClouderaManagerProduct> filterParcelsByBlueprint(Set<ClouderaManagerProduct> parcels, Blueprint blueprint, boolean baseImage) {
        Set<String> serviceNamesInBlueprint = getAllServiceNameInBlueprint(blueprint);
        LOGGER.debug("The following services are found in the blueprint: {}", serviceNamesInBlueprint);
        Set<ClouderaManagerProduct> ret = new HashSet<>();
        if (serviceNamesInBlueprint.contains(null)) {
            LOGGER.debug("We can not identify one of the service from the blueprint so to stay on the safe side we will add every parcel");
            return parcels;
        }
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
        LOGGER.debug("The following parcels are used in CM based on blueprint: {}", ret);
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

    private Set<ClusterComponent> getComponentsByRequiredProducts(Map<String, ClusterComponent> cmProductMap, Set<ClouderaManagerProduct> cmProducts) {
        return cmProducts.stream()
                .map(cmp -> cmProductMap.get(cmp.getName()))
                .collect(Collectors.toSet());
    }
}
