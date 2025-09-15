package com.sequenceiq.cloudbreak.service;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.exception.NotAuditedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackTemplate;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.repository.ComponentRepository;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class ComponentConfigProviderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentConfigProviderService.class);

    private static final String RELEASE_VERSION = "release-version";

    @Inject
    private ComponentRepository componentRepository;

    @Inject
    private TransactionService transactionService;

    @Inject
    private EntityManager entityManager;

    @Nullable
    public Component getComponent(Long stackId, ComponentType componentType, String name) {
        return componentRepository.findComponentByStackIdComponentTypeName(stackId, componentType, name).orElse(null);
    }

    public Set<Component> getAllComponentsByStackIdAndType(Long stackId, Set<ComponentType> componentTypes) {
        return componentRepository.findComponentByStackIdWithType(stackId, componentTypes);
    }

    public Set<Component> getComponentsByStackId(Long stackId) {
        return componentRepository.findComponentByStackId(stackId);
    }

    public Image getImage(Long stackId) throws CloudbreakImageNotFoundException {
        try {
            Component component = getComponent(stackId, ComponentType.IMAGE, ComponentType.IMAGE.name());
            if (component == null) {
                throw new CloudbreakImageNotFoundException(String.format("Image not found: stackId: %d, componentType: %s, name: %s",
                        stackId, ComponentType.IMAGE.name(), ComponentType.IMAGE.name()));
            }
            LOGGER.debug("Image found! stackId: {}, component: {}", stackId, component);
            return component.getAttributes().get(Image.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read image", e);
        }
    }

    public Optional<Image> findImage(Long stackId) {
        Component component = getComponent(stackId, ComponentType.IMAGE, ComponentType.IMAGE.name());
        if (component == null) {
            LOGGER.info("Image not found: stackId: {}, componentType: {}, name: {}", stackId, ComponentType.IMAGE.name(), ComponentType.IMAGE.name());
            return Optional.empty();
        }
        LOGGER.debug("Image found! stackId: {}, component: {}", stackId, component);
        if (component.getAttributes() == null || StringUtils.isBlank(component.getAttributes().getValue())) {
            return Optional.empty();
        }
        try {
            return Optional.of(component.getAttributes().get(Image.class));
        } catch (IOException e) {
            LOGGER.info("Couldn't read image.", e);
            return Optional.empty();
        }
    }

    public Telemetry getTelemetry(Long stackId) {
        try {
            Component component = getComponent(stackId, ComponentType.TELEMETRY, ComponentType.TELEMETRY.name());
            if (component == null) {
                return null;
            }
            return component.getAttributes().get(Telemetry.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read Telemetry for stack.", e);
        }
    }

    public CloudbreakDetails getCloudbreakDetails(Long stackId) {
        try {
            Component component = getComponent(stackId, ComponentType.CLOUDBREAK_DETAILS, ComponentType.CLOUDBREAK_DETAILS.name());
            if (component == null) {
                return null;
            }
            return component.getAttributes().get(CloudbreakDetails.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read Cloudbreak details for stack.", e);
        }
    }

    public void updateStackTemplate(Long stackId, String newTemplate) {
        try {
            Component component = getComponent(stackId, ComponentType.STACK_TEMPLATE, ComponentType.STACK_TEMPLATE.name());
            if (component != null) {
                StackTemplate stackTemplate = component.getAttributes().get(StackTemplate.class);
                stackTemplate.setTemplate(newTemplate);
                component.setAttributes(Json.silent(stackTemplate));
                store(component);
            }
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read template for stack.", e);
        }
    }

    public StackTemplate getStackTemplate(Long stackId) {
        try {
            Component component = getComponent(stackId, ComponentType.STACK_TEMPLATE, ComponentType.STACK_TEMPLATE.name());
            if (component == null) {
                return null;
            }
            return component.getAttributes().get(StackTemplate.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read template for stack.", e);
        }
    }

    public static <T> T getComponent(Collection<Component> components, Class<T> clazz, ComponentType componentType) {
        try {
            Optional<Component> comp = components.stream()
                    .filter(c -> c.getComponentType() == componentType)
                    .filter(c -> c.getName().equals(componentType.name()))
                    .findFirst();
            return comp.isPresent() ? comp.get().getAttributes().get(clazz) : null;
        } catch (IOException e) {
            throw new CloudbreakServiceException(String.format("Failed to read component of type %s", componentType.name()), e);
        }
    }

    public Component store(Component component) {
        LOGGER.debug("Component is going to be saved: {}", component);
        Component ret = componentRepository.save(component);
        LOGGER.debug("Component saved: stackId: {}, component: {}", ret.getStackId(), ret);
        return ret;
    }

    public Iterable<Component> store(Iterable<Component> components) {
        componentRepository.saveAll(components);
        LOGGER.debug("Components saved: {}", components);
        return components;
    }

    public void deleteComponentsForStack(Long stackId) {
        Set<Component> componentsByStackId = getComponentsByStackId(stackId);
        if (!componentsByStackId.isEmpty()) {
            LOGGER.debug("Components({}) are going to be deleted for stack: {}", componentsByStackId.size(), stackId);
            componentRepository.deleteAll(componentsByStackId);
            LOGGER.debug("Components({}) have been deleted for stack : {}", componentsByStackId.size(), stackId);
        }
    }

    public void deleteComponents(Set<Component> components) {
        if (!components.isEmpty()) {
            LOGGER.debug("Components are going to be deleted: {}", components);
            componentRepository.deleteAll(components);
            LOGGER.debug("Components have been deleted: {}", components);
        }
    }

    public void replaceImageComponentWithNew(Component component) {
        Component componentEntity = componentRepository.findComponentByStackIdComponentTypeName(component.getStackId(), component.getComponentType(),
                component.getName()).orElseThrow(NotFoundException.notFound("component", component.getName()));
        componentEntity.setAttributes(component.getAttributes());
        componentEntity.setName(component.getName());
        componentRepository.save(componentEntity);
    }

    public void replaceTelemetryComponent(Long stackId, Telemetry telemetry) {
        ComponentType componentType = ComponentType.TELEMETRY;
        Component componentEntity = componentRepository.findComponentByStackIdComponentTypeName(stackId, componentType,
                componentType.name()).orElseThrow(NotFoundException.notFound("component", componentType.name()));
        componentEntity.setAttributes(new Json(telemetry));
        componentEntity.setName(componentType.name());
        componentRepository.save(componentEntity);
    }

    public Component getImageComponent(Long stackId) throws CloudbreakImageNotFoundException {
        Component component = getComponent(stackId, ComponentType.IMAGE, ComponentType.IMAGE.name());
        if (component == null) {
            throw new CloudbreakImageNotFoundException(String.format("Image not found: stackId: %d, componentType: %s, name: %s",
                    stackId, ComponentType.IMAGE.name(), ComponentType.IMAGE.name()));
        }
        LOGGER.debug("Image found! stackId: {}, component: {}", stackId, component);
        return component;
    }

    public void restoreSecondToLastVersion(Component component) {
        LOGGER.info("Trying to revert to previous version for {}", component);
        try {
            transactionService.required(() -> getRevision(component));
        } catch (NotAuditedException e) {
            LOGGER.warn("Not audited class", e);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Couldn't fetch revision for {}", component, e);
        } catch (Exception e) {
            LOGGER.error("Couldn't revert to previous version for {}", component, e);
        }
    }

    private void getRevision(Component component) {
        AuditReader auditReader = AuditReaderFactory.get(entityManager);
        List<Number> revisions = auditReader.getRevisions(Component.class, component.getId());
        if (!revisions.isEmpty()) {
            Number latestRevision = revisions.get(revisions.size() - 2);
            Component previousImageComponent = auditReader.find(Component.class, component.getId(), latestRevision);
            if (isCurrentRuntimeEqualsOrOlderThanPrevious(component, previousImageComponent)) {
                LOGGER.info("Current runtime version is older than the previous one, no need reverting to previous version. Current: {}, Previous: {}",
                        getRuntimeVersion(component), getRuntimeVersion(previousImageComponent));
                return;
            }
            LOGGER.info("Previous version found: {}", previousImageComponent);
            componentRepository.save(previousImageComponent);
        } else {
            LOGGER.info("No previous version found for {}", component);
        }
    }

    private boolean isCurrentRuntimeEqualsOrOlderThanPrevious(Component component, Component previousImageComponent) {
        String currentRuntimeVersion = getRuntimeVersion(component);
        String previousRuntimeVersion = getRuntimeVersion(previousImageComponent);

        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(() -> currentRuntimeVersion, () -> previousRuntimeVersion) < 1;
    }

    private String getRuntimeVersion(Component imageComponent) {
        String releaseVersion;
        try {
            Image image = imageComponent.getAttributes().get(Image.class);
            releaseVersion = image.getTags().get(RELEASE_VERSION);
            if (StringUtils.isEmpty(releaseVersion)) {
                image.getPackageVersions().get(ImagePackageVersion.STACK.getKey());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return releaseVersion;
    }
}
