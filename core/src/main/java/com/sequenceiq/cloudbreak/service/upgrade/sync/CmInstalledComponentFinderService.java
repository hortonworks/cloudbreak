package com.sequenceiq.cloudbreak.service.upgrade.sync;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ComponentConverter;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

@Service
public class CmInstalledComponentFinderService {

    @Inject
    private ImageReaderService imageReaderService;

    @Inject
    private CmProductChooserService cmProductChooserService;

    @Inject
    private ComponentConverter componentConverter;

    @Inject
    private CmServerQueryService cmServerQueryService;

    void findParcelComponents(Stack stack, Set<StatedImage> candidateImages, Set<Component> syncedFromServer) {
        Set<ClouderaManagerProduct> candidateProducts = imageReaderService.getParcels(candidateImages, stack.isDatalake());
        Set<ParcelInfo> installedParcels = cmServerQueryService.queryActiveParcels(stack);
        Set<ClouderaManagerProduct> installedProducts = cmProductChooserService.chooseParcelProduct(installedParcels, candidateProducts);
        syncedFromServer.addAll(componentConverter.fromClouderaManagerProductList(installedProducts, stack));
    }

    void findCmRepoComponent(Stack stack, Set<StatedImage> candidateImages, Set<Component> syncedComponents) {
        Set<ClouderaManagerRepo> candidateCmRepos = imageReaderService.getCmRepos(candidateImages);
        String cmVersion = cmServerQueryService.queryCmVersion(stack);
        cmProductChooserService.chooseCmRepo(cmVersion, candidateCmRepos)
                .map(cmRepo -> componentConverter.fromClouderaManagerRepo(cmRepo, stack))
                .ifPresent(syncedComponents::add);
    }

}
