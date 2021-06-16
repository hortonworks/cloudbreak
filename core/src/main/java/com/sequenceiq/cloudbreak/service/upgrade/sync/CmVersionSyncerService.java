package com.sequenceiq.cloudbreak.service.upgrade.sync;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ClouderaManagerProductConverter;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterComponentUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.StackComponentUpdater;

@Service
public class CmVersionSyncerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmVersionSyncerService.class);

    @Inject
    private ClouderaManagerProductFinderService clouderaManagerProductFinderService;

    @Inject
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Inject
    private StackComponentUpdater stackComponentUpdater;

    @Inject
    private ClouderaManagerProductConverter clouderaManagerProductConverter;

    @Inject
    private ClusterComponentUpdater clusterComponentUpdater;

    @Inject
    private CmParcelInfoRetrieverService cmParcelInfoRetriever;

    /**
     * Will retrieve all the active parcels from the CM server and try to find matching products in stated images.
     * Found products will then be used to update the Component and ClusterComponent table in DB
     *
     * @param stack The stack that needs to be synced
     * @param candidateImages Set of candidate images whose products are used to match against the parcel version received from CM server.
     */
    public void syncCmParcelsToDb(Stack stack, Set<StatedImage> candidateImages) {
        List<ClouderaManagerProduct> candidateProducts = candidateImages.stream()
                .map(ci -> clouderaManagerProductTransformer.transform(ci.getImage(), !stack.isDatalake()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        List<ParcelInfo> installedParcels = cmParcelInfoRetriever.getActiveParcelsFromServer(stack.getId());
        List<ClouderaManagerProduct> installedProducts = clouderaManagerProductFinderService.findInstalledProduct(installedParcels, candidateProducts);
        Set<Component> installedComponents = clouderaManagerProductConverter.clouderaManagerProductListToComponent(installedProducts, stack);
        LOGGER.debug("Active components read from CM server and persisting now to the DB: {}", installedProducts);
        stackComponentUpdater.updateComponentsByStackId(stack, installedComponents, false);
        clusterComponentUpdater.updateClusterComponentsByStackId(stack, installedComponents, false);
    }

}
