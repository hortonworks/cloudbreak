package com.sequenceiq.cloudbreak.cloud.model.component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.PreWarmParcelParser;
import com.sequenceiq.cloudbreak.service.image.StatedImages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ImageBasedDefaultCDHEntries {

    public static final Comparator<Map.Entry<String, ImageBasedDefaultCDHInfo>> IMAGE_BASED_CDH_ENTRY_COMPARATOR =
            Comparator.comparing(Map.Entry::getKey);

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageBasedDefaultCDHEntries.class);

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private PreWarmParcelParser preWarmParcelParser;

    public Map<String, ImageBasedDefaultCDHInfo> getEntries(Long workspaceId, String platform, String imageCatalogName) throws CloudbreakImageCatalogException {
        String catalogName = Optional.ofNullable(imageCatalogName).orElse(ImageCatalogService.CDP_DEFAULT_CATALOG_NAME);
        StatedImages images = imageCatalogService.getImages(workspaceId, catalogName, platform);
        if (images.getImages().getCdhImages().isEmpty()) {
            LOGGER.warn("Missing CDH images for cloud platform: {}. Falling back to AWS.", platform);
            images = imageCatalogService.getImages(workspaceId, catalogName, CloudPlatform.AWS.name());
        }

        return getEntries(images.getImages());
    }

    public Map<String, ImageBasedDefaultCDHInfo> getEntries(Images images) {
        return images.getCdhImages().stream()
                .filter(Image::isDefaultImage)
                .collect(Collectors.toMap(Image::getVersion, i -> new ImageBasedDefaultCDHInfo(getDefaultCDHInfo(i), i),
                        //The generated CDHInfo should be the same for the same version so it does not matter which one is used.
                        //It can happend when calling for an image catalog with images.
                        (i1, i2) -> i1));
    }

    private DefaultCDHInfo getDefaultCDHInfo(Image image) {
        DefaultCDHInfo defaultCdhInfo = new DefaultCDHInfo();
        defaultCdhInfo.setVersion(image.getStackDetails().getRepo().getStack().get(StackRepoDetails.REPOSITORY_VERSION));
        defaultCdhInfo.setRepo(getRepoDetails(image));
        defaultCdhInfo.setParcels(getParcels(image));
        defaultCdhInfo.setCsd(image.getPreWarmCsd());
        return defaultCdhInfo;
    }

    private List<ClouderaManagerProduct> getParcels(Image image) {
        return image.getPreWarmParcels()
                .stream()
                .map(parcel -> preWarmParcelParser.parseProductFromParcel(parcel, image.getPreWarmCsd()))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    private ClouderaManagerDefaultStackRepoDetails getRepoDetails(Image image) {
        ClouderaManagerDefaultStackRepoDetails repoDetails = new ClouderaManagerDefaultStackRepoDetails();
        Map<String, String> repoStack = new HashMap(image.getStackDetails().getRepo().getStack());
        repoStack.remove(StackRepoDetails.REPOSITORY_VERSION);
        repoDetails.setStack(repoStack);

        return repoDetails;
    }
}
