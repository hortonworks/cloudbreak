package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static java.util.Collections.singletonMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintHybridOption;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.CentralCDHVersionCoordinator;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class CsdParcelDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsdParcelDecorator.class);

    @Inject
    private ParcelService parcelService;

    @Inject
    private CentralCDHVersionCoordinator centralCDHVersionCoordinator;

    @Inject
    private EntitlementService entitlementService;

    public void decoratePillarWithCsdParcels(StackDto stackDto, Map<String, SaltPillarProperties> servicePillar) {
        StackView stack = stackDto.getStack();
        if (shouldDecorateWithCsdParcels(stackDto)) {
            LOGGER.debug("Decorating service pillar with CSD parcels.");
            Set<ClusterComponentView> componentsByBlueprint = parcelService.getParcelComponentsByBlueprint(stackDto);
            servicePillar.putAll(addCsdParcelsToServicePillar(
                    centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(componentsByBlueprint), false));
        } else {
            LOGGER.debug("Skipping the CSD downloading because the stack type is {}", stack.getType());
        }
    }

    private boolean shouldDecorateWithCsdParcels(StackDto stackDto) {
        if (StackType.WORKLOAD.equals(stackDto.getType())) {
            return true;
        }
        if (entitlementService.isOnPremBaseDlShapeEnabled(stackDto.getAccountId())) {
            BlueprintHybridOption hybridOption = Optional.ofNullable(stackDto.getBlueprint())
                    .map(Blueprint::getHybridOption)
                    .orElse(BlueprintHybridOption.NONE);
            LOGGER.debug("Checking for blueprint hybrid option {} to determine if csd download is needed", hybridOption);
            return hybridOption == BlueprintHybridOption.ON_PREM_BASE;
        }
        return false;
    }

    public Map<String, SaltPillarProperties> addCsdParcelsToServicePillar(Collection<ClouderaManagerProduct> products, boolean upgradePreparation) {
        Map<String, SaltPillarProperties> pillarPropertiesMap = new HashMap<>();
        List<String> csdUrls = getCsdUrlList(products);
        if (!csdUrls.isEmpty()) {
            LOGGER.debug("Decorating pillar with the following CSD parcels: {}", csdUrls);
            pillarPropertiesMap.put("csd-downloader", new SaltPillarProperties("/cloudera-manager/csd.sls",
                    singletonMap("cloudera-manager",
                            Map.of("csd-urls", csdUrls, "upgrade-preparation", upgradePreparation))));
        } else {
            LOGGER.debug("There are no CSD to add to the pillar. Related products: {}", products);
        }
        return pillarPropertiesMap;
    }

    private List<String> getCsdUrlList(Collection<ClouderaManagerProduct> products) {
        return products.stream()
                .map(ClouderaManagerProduct::getCsd)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
