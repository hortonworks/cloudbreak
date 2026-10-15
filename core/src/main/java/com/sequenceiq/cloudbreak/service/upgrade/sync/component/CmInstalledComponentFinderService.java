package com.sequenceiq.cloudbreak.service.upgrade.sync.component;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.sync.CustomParcelFilterService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmParcelSyncOperationResult;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmRepoSyncOperationResult;

@Service
public class CmInstalledComponentFinderService {

    @Inject
    private ImageReaderService imageReaderService;

    @Inject
    private CmProductChooserService cmProductChooserService;

    @Inject
    private CmServerQueryService cmServerQueryService;

    @Inject
    private CustomParcelFilterService customParcelFilterService;

    public CmRepoSyncOperationResult findCmRepoComponent(Stack stack, Set<Image> candidateImages, ClusterUpgradeProperties properties) {
        Set<ClouderaManagerRepo> candidateCmRepos = new LinkedHashSet<>();
        if (properties != null) {
            Optional.ofNullable(properties.targetImage().clouderaManagerRepo()).ifPresent(candidateCmRepos::add);
            Optional.ofNullable(properties.currentImage()).map(ClusterUpgradeProperties.CurrentImageUpgradeContext::clouderaManagerRepo)
                    .ifPresent(candidateCmRepos::add);
        }
        candidateCmRepos.addAll(imageReaderService.getCmRepos(candidateImages));
        Optional<String> cmVersionOptional = cmServerQueryService.queryCmVersion(stack);
        Optional<ClouderaManagerRepo> clouderaManagerRepoOptional = cmProductChooserService.chooseCmRepo(cmVersionOptional, candidateCmRepos);
        return new CmRepoSyncOperationResult(cmVersionOptional.orElse(null), clouderaManagerRepoOptional.orElse(null));
    }

    public CmParcelSyncOperationResult findParcelComponents(Stack stack, Set<Image> candidateImages, ClusterUpgradeProperties properties) {
        Set<ClouderaManagerProduct> candidateProducts = new LinkedHashSet<>();
        if (properties != null) {
            candidateProducts.addAll(properties.targetImage().getAllProducts());
            Optional.ofNullable(properties.currentImage()).map(ClusterUpgradeProperties.CurrentImageUpgradeContext::products)
                    .ifPresent(candidateProducts::addAll);
        }
        candidateProducts.addAll(imageReaderService.getParcels(candidateImages, stack.isDatalake()));
        Set<ParcelInfo> activeParcels = getActiveParcels(stack, candidateProducts);
        Set<ClouderaManagerProduct> activeProducts = cmProductChooserService.chooseParcelProduct(activeParcels, candidateProducts);
        return new CmParcelSyncOperationResult(activeParcels, activeProducts);
    }

    private Set<ParcelInfo> getActiveParcels(Stack stack, Set<ClouderaManagerProduct> candidateProducts) {
        Set<ParcelInfo> activeParcels = cmServerQueryService.queryActiveParcels(stack);
        return customParcelFilterService.filterCustomParcels(activeParcels, candidateProducts);
    }
}
