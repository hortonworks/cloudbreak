package com.sequenceiq.cloudbreak.service.upgrade.sync.component;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;

@Service
public class ImageReaderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageReaderService.class);

    @Inject
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Inject
    private ImageService imageService;

    /**
     * Will extract the parcel info from the supplied images. If the stack is a datalake, then only the CDH parcel is extracted.
     * @param statedImages images to get parcels from
     * @param datalake true if the stack is a datalake
     * @return a set of ClouderaManagerProducts present on the image
     */
    @Measure(ImageReaderService.class)
    Set<ClouderaManagerProduct> getParcels(Set<Image> statedImages, boolean datalake) {
        Set<ClouderaManagerProduct> foundParcelProducts = statedImages.stream()
                .map(im -> clouderaManagerProductTransformer.transform(im, true, !datalake))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        LOGGER.debug("Found following candidate parcels: {}", foundParcelProducts);
        return foundParcelProducts;
    }

    /**
     * Will extract the CM repos from the supplied images
     * @param images images to get CM repos
     * @return a set of found CM repos
     */
    @Measure(ImageReaderService.class)
    Set<ClouderaManagerRepo> getCmRepos(Set<Image> images) {
        Set<ClouderaManagerRepo> foundClouderaManagerRepos = images.stream()
                .map(this::getCmComponent)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
        LOGGER.debug("Found following CM repos in images: {}", foundClouderaManagerRepos);
        return foundClouderaManagerRepos;
    }

    private Optional<ClouderaManagerRepo> getCmComponent(Image image) {
        try {
            return imageService.getClouderaManagerRepo(image);
        } catch (CloudbreakImageCatalogException e) {
            LOGGER.info("Failed to get CM component from stated image {}", image);
            return Optional.empty();
        }
    }

}
