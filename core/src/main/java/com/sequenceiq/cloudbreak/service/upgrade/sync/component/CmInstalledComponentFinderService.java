package com.sequenceiq.cloudbreak.service.upgrade.sync.component;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.upgrade.sync.common.ParcelInfo;
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

    public CmRepoSyncOperationResult findCmRepoComponent(Stack stack, Set<Image> candidateImages) {
        Set<ClouderaManagerRepo> candidateCmRepos = imageReaderService.getCmRepos(candidateImages);
        Optional<String> cmVersionOptional = cmServerQueryService.queryCmVersion(stack);
        Optional<ClouderaManagerRepo> clouderaManagerRepoOptional = cmProductChooserService.chooseCmRepo(cmVersionOptional, candidateCmRepos);
        return new CmRepoSyncOperationResult(cmVersionOptional.orElse(null), clouderaManagerRepoOptional.orElse(null));
    }

    public CmParcelSyncOperationResult findParcelComponents(Stack stack, Set<Image> candidateImages) {
        Set<ClouderaManagerProduct> candidateProducts = imageReaderService.getParcels(candidateImages, stack.isDatalake());
        Set<ParcelInfo> installedParcels = cmServerQueryService.queryActiveParcels(stack);
        Set<ClouderaManagerProduct> installedProducts = cmProductChooserService.chooseParcelProduct(installedParcels, candidateProducts);
        return new CmParcelSyncOperationResult(installedParcels, installedProducts);
    }

}
