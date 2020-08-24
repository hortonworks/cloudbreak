package com.sequenceiq.cloudbreak.service.parcel;

import java.io.IOException;
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

import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedService;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.exception.NotFoundException;

@Service
public class ParcelService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcelService.class);

    @Inject
    private CmTemplateGeneratorService clusterTemplateGeneratorService;

    @Inject
    private RestClientFactory restClientFactory;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    public Set<ClusterComponent> getParcelComponentsByBlueprint(Stack stack) {
        Cluster cluster = stack.getCluster();
        Set<ClusterComponent> components = getParcelComponents(cluster);
        if (stack.isDatalake()) {
            ClusterComponent stackComponent = components.stream()
                    .filter(clusterComponent -> clusterComponent.getName().equals(StackType.CDH.name()))
                    .findFirst().orElseThrow(() -> new NotFoundException("Runtime component not found!"));
            LOGGER.debug("For datalake clusters only the CDH parcel is used in CM: {}", stackComponent);
            return Set.of(stackComponent);
        } else {
            Map<String, ClusterComponent> cmProductMap = new HashMap<>();
            Set<ClouderaManagerProduct> cmProducts = new HashSet<>();
            for (ClusterComponent clusterComponent : components) {
                ClouderaManagerProduct product = clusterComponent.getAttributes().getSilent(ClouderaManagerProduct.class);
                cmProductMap.put(product.getName(), clusterComponent);
                cmProducts.add(product);
            }
            cmProducts = filterParcelsByBlueprint(cmProducts, cluster.getBlueprint(), false);
            LOGGER.debug("The following parcels are used in CM based on blueprint: {}", cmProducts);
            return cmProducts.stream().map(cmp -> cmProductMap.get(cmp.getName())).collect(Collectors.toSet());
        }
    }

    public Set<ClouderaManagerProduct> filterParcelsByBlueprint(Set<ClouderaManagerProduct> parcels, Blueprint blueprint, boolean baseImage) {
        Set<String> serviceNamesInBlueprint = getAllServiceNameInBlueprint(blueprint);
        Set<ClouderaManagerProduct> ret = new HashSet<>();
        if (serviceNamesInBlueprint.contains(null)) {
            // We can not identify one of the service from the blueprint so to stay on the safe side we will add every parcel
            return parcels;
        }
        parcels.forEach(parcel -> {
            ImmutablePair<ManifestStatus, Manifest> manifest = readRepoManifest(parcel.getParcel());
            if (manifest.right != null && ManifestStatus.SUCCESS.equals(manifest.left)) {
                Set<String> componentNamesInParcel = getAllComponentNameInParcel(manifest.right);
                if (componentNamesInParcel.stream().anyMatch(serviceNamesInBlueprint::contains)) {
                    ret.add(parcel);
                }
            } else if (ManifestStatus.COULD_NOT_PARSE.equals(manifest.left)) {
                ret.add(parcel);
            } else if (ManifestStatus.FAILED.equals(manifest.left) && !baseImage) {
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
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    private ImmutablePair<ManifestStatus, Manifest> readRepoManifest(String baseUrl) {
        String content = null;
        try {
            Client client = restClientFactory.getOrCreateDefault();
            WebTarget target = client.target(StringUtils.stripEnd(baseUrl, "/") + "/manifest.json");
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

    private Set<ClusterComponent> getParcelComponents(Cluster cluster) {
        return clusterComponentConfigProvider.getComponentsByClusterId(cluster.getId()).stream()
                .filter(clusterComponent -> ComponentType.CDH_PRODUCT_DETAILS == clusterComponent.getComponentType())
                .collect(Collectors.toSet());
    }
}
