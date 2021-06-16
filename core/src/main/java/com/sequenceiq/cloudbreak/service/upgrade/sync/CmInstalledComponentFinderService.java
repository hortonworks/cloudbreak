package com.sequenceiq.cloudbreak.service.upgrade.sync;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ComponentConverter;

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

    Set<Component> findParcelComponents(Stack stack, Set<Image> candidateImages) {
        Set<ClouderaManagerProduct> candidateProducts = imageReaderService.getParcels(candidateImages, stack.isDatalake());
        Set<ParcelInfo> installedParcels = cmServerQueryService.queryActiveParcels(stack);
        Set<ClouderaManagerProduct> installedProducts = cmProductChooserService.chooseParcelProduct(installedParcels, candidateProducts);
        return componentConverter.fromClouderaManagerProductList(installedProducts, stack);
    }

    Optional<Component> findCmRepoComponent(Stack stack, Set<Image> candidateImages) {
        Set<ClouderaManagerRepo> candidateCmRepos = imageReaderService.getCmRepos(candidateImages);
        String cmVersion = cmServerQueryService.queryCmVersion(stack);
        return cmProductChooserService.chooseCmRepo(cmVersion, candidateCmRepos)
                .map(cmRepo -> componentConverter.fromClouderaManagerRepo(cmRepo, stack));
    }

}
