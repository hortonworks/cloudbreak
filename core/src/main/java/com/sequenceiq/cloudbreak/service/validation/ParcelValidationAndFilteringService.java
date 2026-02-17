package com.sequenceiq.cloudbreak.service.validation;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.parcel.ParcelFilterService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Service
public class ParcelValidationAndFilteringService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParcelValidationAndFilteringService.class);

    @Inject
    private ParcelFilterService parcelFilterService;

    public void validate(Stack stack, ValidationResult.ValidationResultBuilder validationBuilder) {
        if (stack.isDatahub()) {
            try {
                LOGGER.debug("Validating parcels for the cluster");
                Set<ClouderaManagerProduct> stackCdhRepoConfigs = getClouderaManagerProductsFromStackComponents(stack);
                Set<ClouderaManagerProduct> filteredProducts = parcelFilterService
                        .filterParcelsByBlueprint(stack.getWorkspace().getId(), stack.getId(), stackCdhRepoConfigs, stack.getCluster().getBlueprint());
                LOGGER.info("Product list after filtering out products by blueprint: {}", filteredProducts);
                logNonBlueprintRequiredButConfiguredParcels(stackCdhRepoConfigs, filteredProducts);
            } catch (CloudbreakRuntimeException cloudbreakRuntimeException) {
                LOGGER.warn("Parcel validation failed: {}", cloudbreakRuntimeException.getMessage());
                validationBuilder.error("The validation of the configured parcels for the cluster has failed: " + cloudbreakRuntimeException.getMessage());
            } catch (IllegalStateException illegalStateException) {
                LOGGER.warn("Parcel validation failed because stack component for CDH product is invalid: {}", illegalStateException.getMessage());
                validationBuilder.error("The validation of the configured parcels for the cluster has failed: " + illegalStateException.getMessage());
            }
        }
    }

    private static Set<ClouderaManagerProduct> getClouderaManagerProductsFromStackComponents(Stack stack) {
        return stack.getComponents().stream()
                .filter(c -> ComponentType.cdhProductDetails().equals(c.getComponentType()))
                .map(Component::getAttributes)
                .map(json -> json.getUnchecked(ClouderaManagerProduct.class))
                .collect(Collectors.toUnmodifiableSet());
    }

    private void logNonBlueprintRequiredButConfiguredParcels(Set<ClouderaManagerProduct> stackCdhRepoConfigs, Set<ClouderaManagerProduct> filteredProducts) {
        SetView<ClouderaManagerProduct> nonUsedCdhRepoConfigsByBlueprint = Sets.difference(stackCdhRepoConfigs, filteredProducts);
        if (!nonUsedCdhRepoConfigsByBlueprint.isEmpty()) {
            LOGGER.warn("The following parcels are configured but not used by the blueprint: {}", nonUsedCdhRepoConfigsByBlueprint);
        }
    }
}
