package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static java.util.Collections.singletonMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
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

    public void decoratePillarWithCsdParcels(StackDto stackDto, Map<String, SaltPillarProperties> servicePillar) {
        StackView stack = stackDto.getStack();
        if (StackType.WORKLOAD.equals(stack.getType())) {
            LOGGER.debug("Decorating service pillar with CSD parcels.");
            Set<ClusterComponentView> componentsByBlueprint = parcelService.getParcelComponentsByBlueprint(stackDto);
            addCsdParcelsToServicePillar(
                    centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(componentsByBlueprint),
                    servicePillar);
        } else {
            LOGGER.debug("Skipping the CSD downloading because the stack type is {}", stack.getType());
        }
    }

    private void addCsdParcelsToServicePillar(Collection<ClouderaManagerProduct> products, Map<String, SaltPillarProperties> servicePillar) {
        List<String> csdUrls = getCsdUrlList(products);
        if (!csdUrls.isEmpty()) {
            LOGGER.debug("Decorating pillar with the following CSD parcels: {}", csdUrls);
            servicePillar.put("csd-downloader", new SaltPillarProperties("/cloudera-manager/csd.sls",
                    singletonMap("cloudera-manager",
                            singletonMap("csd-urls", csdUrls))));
        } else {
            LOGGER.debug("There are no CSD to add to the pillar. Related products: {}", products);
        }
    }

    private List<String> getCsdUrlList(Collection<ClouderaManagerProduct> products) {
        return products.stream()
                .map(ClouderaManagerProduct::getCsd)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
