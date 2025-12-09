package com.sequenceiq.cloudbreak.cluster.service;

import static com.sequenceiq.cloudbreak.common.type.ComponentType.cdhProductDetails;
import static com.sequenceiq.cloudbreak.util.Benchmark.measureAndWarnIfLong;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.apache.commons.codec.binary.Base64;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.exception.NotAuditedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponentHistory;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.repository.ClusterComponentHistoryRepository;
import com.sequenceiq.cloudbreak.repository.ClusterComponentRepository;
import com.sequenceiq.cloudbreak.repository.ClusterComponentViewRepository;
import com.sequenceiq.cloudbreak.util.CdhVersionProvider;

@Service
public class ClusterComponentConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterComponentConfigProvider.class);

    private ClusterComponentRepository componentRepository;

    private ClusterComponentViewRepository componentViewRepository;

    private ClusterComponentHistoryRepository clusterComponentHistoryRepository;

    private EntityManager entityManager;

    private TransactionService transactionService;

    public ClusterComponentConfigProvider(ClusterComponentRepository componentRepository,
            ClusterComponentViewRepository componentViewRepository,
            ClusterComponentHistoryRepository clusterComponentHistoryRepository,
            EntityManager entityManager, TransactionService transactionService) {
        this.componentRepository = componentRepository;
        this.componentViewRepository = componentViewRepository;
        this.clusterComponentHistoryRepository = clusterComponentHistoryRepository;
        this.entityManager = entityManager;
        this.transactionService = transactionService;
    }

    public ClusterComponent getComponent(Long clusterId, ComponentType componentType) {
        return getComponent(clusterId, componentType, componentType.name());
    }

    public ClusterComponent getComponent(Long clusterId, ComponentType componentType, String name) {
        return componentRepository.findComponentByClusterIdComponentTypeName(clusterId, componentType, name);
    }

    public Set<ClusterComponentView> getComponentListByType(Long clusterId, ComponentType componentType) {
        return componentViewRepository.findComponentViewsByClusterIdAndComponentType(clusterId, componentType);
    }

    public Set<ClusterComponent> getComponentsByClusterId(Long clusterId) {
        return componentRepository.findComponentByClusterId(clusterId);
    }

    public Set<ClusterComponentView> getComponentsByClusterIdAndInComponentType(Long clusterId, Collection<ComponentType> types) {
        return componentViewRepository.findComponentsByClusterIdAndInComponentType(clusterId, types);
    }

    public ClouderaManagerRepo getClouderaManagerRepoDetails(Collection<ClusterComponent> clusterComponents) {
        ClusterComponent component = clusterComponents.stream().filter(cc -> cc.getComponentType().equals(ComponentType.CM_REPO_DETAILS)).findFirst()
                .orElseThrow(NotFoundException.notFound("Cannot find Cloudera Manager Repo details for the cluster"));
        return retrieveFromAttribute(component, ClouderaManagerRepo.class);
    }

    public ClouderaManagerRepo getClouderaManagerRepoDetails(Long clusterId) {
        ClusterComponent component = getComponent(clusterId, ComponentType.CM_REPO_DETAILS);
        return retrieveFromAttribute(component, ClouderaManagerRepo.class);
    }

    public StackRepoDetails getStackRepoDetails(Long clusterId) {
        ClusterComponent component = ofNullable(getComponent(clusterId, ComponentType.HDP_REPO_DETAILS))
                .orElse(getComponent(clusterId, ComponentType.HDF_REPO_DETAILS));
        return retrieveFromAttribute(component, StackRepoDetails.class);
    }

    public List<ClouderaManagerProduct> getClouderaManagerProductDetails(Long clusterId) {
        Set<ClusterComponentView> components = getComponentListByType(clusterId, cdhProductDetails());
        return components.stream()
                .map(component -> retrieveFromAttribute(component, ClouderaManagerProduct.class))
                .collect(Collectors.toList());
    }

    public Optional<ClouderaManagerProduct> getCdhProduct(Long clusterId) {
        List<ClouderaManagerProduct> clouderaManagerProductDetails = getClouderaManagerProductDetails(clusterId);
        if (clouderaManagerProductDetails != null) {
            return clouderaManagerProductDetails
                    .stream()
                    .filter(p -> "CDH".equals(p.getName()))
                    .findAny();
        }
        return Optional.empty();
    }

    public Optional<ClouderaManagerProduct> getNormalizedCdhProductWithNormalizedVersion(Long clusterId) {
        Optional<ClouderaManagerProduct> product = getCdhProduct(clusterId);
        if (product.isPresent()) {
            ClouderaManagerProduct prod = product.get();
            CdhVersionProvider.getCdhStackVersionFromVersionString(prod.getVersion()).ifPresent(prod::setVersion);
        }
        return product;
    }

    public byte[] getSaltStateComponent(Long clusterId) {
        byte[] result = null;
        ClusterComponent component = ofNullable(getComponent(clusterId, ComponentType.SALT_STATE)).orElse(null);
        if (component != null && ComponentType.SALT_STATE.equals(component.getComponentType())) {
            Json jsonAttr = component.getAttributes();
            if (jsonAttr != null) {
                Map<String, Object> saltStateMap = jsonAttr.getMap();
                String key = ComponentType.SALT_STATE.name();
                if (saltStateMap.containsKey(key) && saltStateMap.get(key) != null) {
                    String base64Content = saltStateMap.get(key).toString();
                    result = Base64.decodeBase64(base64Content);
                }
            }
        }
        return result;
    }

    public String getSaltStateComponentCbVersion(Long clusterId) {
        String result = null;
        ClusterComponent component = getComponent(clusterId, ComponentType.SALT_STATE);
        if (component != null && ComponentType.SALT_STATE.equals(component.getComponentType())) {
            Json jsonAttr = component.getAttributes();
            if (jsonAttr != null) {
                Map<String, Object> jsonMap = jsonAttr.getMap();
                if (jsonMap.containsKey(ClusterComponent.CB_VERSION_KEY) && jsonMap.get(ClusterComponent.CB_VERSION_KEY) != null) {
                    result = jsonMap.get(ClusterComponent.CB_VERSION_KEY).toString();
                }
            }
        }
        return result;
    }

    public <T> T getComponent(Collection<ClusterComponent> components, Class<T> clazz, ComponentType componentType, String name) {
        try {
            Optional<ClusterComponent> comp = components.stream()
                    .filter(c -> c.getComponentType() == componentType)
                    .filter(c -> c.getName().equals(name))
                    .findFirst();
            return comp.isPresent() ? comp.get().getAttributes().get(clazz) : null;
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read component", e);
        }
    }

    public ClusterComponent store(ClusterComponent component) {
        LOGGER.debug("Component is going to be saved: {}", component);
        ClusterComponent ret = componentRepository.save(component);
        LOGGER.debug("Component saved: clusterId: {}, component: {}", ret.getCluster().getId(), ret);
        return ret;
    }

    public Iterable<ClusterComponent> store(Iterable<ClusterComponent> components) {
        return componentRepository.saveAll(components);
    }

    public List<ClusterComponent> store(Collection<ClusterComponent> components, Cluster cluster) {
        List<ClusterComponent> ret = new ArrayList<>();
        components.forEach(comp -> comp.setCluster(cluster));
        componentRepository.saveAll(components).forEach(ret::add);
        LOGGER.debug("Components saved: stackId: {}, components: {}", cluster.getId(), ret);
        return ret;
    }

    public void restorePreviousVersion(ClusterComponent clusterComponent) {
        LOGGER.info("Trying to revert to previous version for {}", clusterComponent);
        try {
            transactionService.required(() -> getRevision(clusterComponent));
        } catch (NotAuditedException e) {
            LOGGER.warn("Not audited class", e);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Couldn't fetch revision for {}", clusterComponent, e);
        } catch (Exception e) {
            LOGGER.error("Couldn't revert to previous version for {}", clusterComponent, e);
        }
    }

    private void getRevision(ClusterComponent clusterComponent) {
        AuditReader auditReader = AuditReaderFactory.get(entityManager);
        List<Number> revisions = auditReader.getRevisions(ClusterComponent.class, clusterComponent.getId());
        if (!revisions.isEmpty()) {
            // @see AuditReader: list of revision numbers, at which the entity was modified, sorted in ascending order
            Number latestRevision = revisions.get(revisions.size() - 1);
            ClusterComponent previousClusterComponent = auditReader.find(ClusterComponent.class, clusterComponent.getId(), latestRevision);
            LOGGER.info("Previous version found: {}", previousClusterComponent);
            componentRepository.save(previousClusterComponent);
        } else {
            LOGGER.info("No previous version found for {}", clusterComponent);
        }
    }

    public void cleanUpDetachedEntries(Long clusterId) {
        LOGGER.debug("About to delete detached {} entries (for cluster: {}) if they exist.", clusterId, ClusterComponentHistory.class.getSimpleName());
        measureAndWarnIfLong(() -> clusterComponentHistoryRepository.deleteByClusterId(clusterId), LOGGER,
                "Cleaning up detached ClusterComponentHistory entries");
    }

    public void deleteClusterComponents(Set<ClusterComponent> components) {
        if (isNotEmpty(components)) {
            LOGGER.debug("Components are going to be deleted: {}", components);
            componentRepository.deleteAll(components);
            LOGGER.debug("Components have been deleted: {}", components);
        } else {
            LOGGER.debug("Empty/null set of {}s has been passed for deletion, therefore no deletion happened.", ClusterComponent.class.getSimpleName());
        }
    }

    public void deleteClusterComponentViews(Set<ClusterComponentView> components) {
        if (!components.isEmpty()) {
            LOGGER.debug("Components are going to be deleted: {}", components);
            components.forEach(c -> componentRepository.deleteById(c.getId()));
            LOGGER.debug("Components have been deleted: {}", components);
        }
    }

    public void deleteClusterComponentByClusterIdAndComponentType(long clusterId, ComponentType componentType) {
        LOGGER.debug("Component with cluster id {} and component type {} is going to be deleted", clusterId, componentType);
        componentRepository.deleteComponentByClusterIdAndComponentType(clusterId, componentType);
        LOGGER.debug("Component with cluster id {} and component type {} is deleted", clusterId, componentType);
    }

    private <T> T retrieveFromAttribute(ClusterComponent component, Class<T> clazz) {
        if (component == null) {
            return null;
        }
        return retrieveFromAttributeJson(component.getAttributes(), clazz);
    }

    private <T> T retrieveFromAttribute(ClusterComponentView componentView, Class<T> clazz) {
        if (componentView == null) {
            return null;
        }
        return retrieveFromAttributeJson(componentView.getAttributes(), clazz);
    }

    private <T> T retrieveFromAttributeJson(Json attributes, Class<T> clazz) {
        if (attributes == null) {
            return null;
        }
        try {
            return attributes.get(clazz);
        } catch (IOException e) {
            String message = String.format("Failed to read attributes json into class: %s", clazz);
            LOGGER.debug(message);
            throw new CloudbreakServiceException(message, e);
        }
    }

}
