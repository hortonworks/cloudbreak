package com.sequenceiq.cloudbreak.service.parcel;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.CentralCDHVersionCoordinator;

@Component
public class UpgradeCandidateProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCandidateProvider.class);

    @Inject
    private CentralCDHVersionCoordinator centralCDHVersionCoordinator;

    public Set<ClouderaManagerProduct> getRequiredProductsForUpgrade(ClusterApi connector, StackDto stackDto, Set<ClusterComponentView> componentsByBlueprint) {
        Set<ParcelInfo> activeParcels = getActiveParcels(connector, stackDto);
        Set<ClouderaManagerProduct> products = centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(componentsByBlueprint);
        Set<ClouderaManagerProduct> upgradeCandidates = findUpgradeCandidates(products, activeParcels);
        LOGGER.debug("Active parcels: {}, Required parcels for the cluster: {}, Upgrade candidate parcels: {}", activeParcels, products, upgradeCandidates);
        return upgradeCandidates;
    }

    private Set<ClouderaManagerProduct> findUpgradeCandidates(Set<ClouderaManagerProduct> products, Set<ParcelInfo> activeParcels) {
        return products.stream()
                .filter(upgradeCandidateProduct -> activeParcels.stream()
                        .noneMatch(parcel -> parcel.getName().equals(upgradeCandidateProduct.getName())
                                && parcel.getVersion().equals(upgradeCandidateProduct.getVersion())))
                .collect(Collectors.toSet());
    }

    private Set<ParcelInfo> getActiveParcels(ClusterApi connector, StackDto stackDto) {
        return connector.gatherInstalledParcels(stackDto.getName());
    }
}
