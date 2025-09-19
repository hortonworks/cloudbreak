package com.sequenceiq.cloudbreak.cloud.model.component;

import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageOsService;
import com.sequenceiq.cloudbreak.service.image.PreWarmParcelParser;
import com.sequenceiq.cloudbreak.service.image.StatedImages;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@Component
public class ImageBasedDefaultCDHEntries {

    public static final Comparator<Map.Entry<String, ImageBasedDefaultCDHInfo>> IMAGE_BASED_CDH_ENTRY_COMPARATOR =
            Comparator.comparing(Map.Entry::getKey);

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageBasedDefaultCDHEntries.class);

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private PreWarmParcelParser preWarmParcelParser;

    @Inject
    private ImageOsService imageOsService;

    public Map<String, ImageBasedDefaultCDHInfo> getEntries(Long workspaceId, ImageCatalogPlatform platform, String os, Architecture architecture,
            String imageCatalogName) throws CloudbreakImageCatalogException {
        String catalogName = StringUtils.defaultIfEmpty(imageCatalogName, ImageCatalogService.CDP_DEFAULT_CATALOG_NAME);
        StatedImages images = imageCatalogService.getImages(workspaceId, catalogName, null, platform, true, null);
        if (images.getImages().getCdhImages().isEmpty()) {
            LOGGER.warn("Missing CDH images for cloud platform: {}. Falling back to AWS.", platform);
            images = imageCatalogService.getImages(workspaceId, catalogName, null, imageCatalogPlatform(CloudPlatform.AWS.name()), true, null);
        }

        return getEntries(images.getImages(), os, architecture);
    }

    public Map<String, ImageBasedDefaultCDHInfo> getEntries(Images images) {
        return getEntries(images, null, null);
    }

    public Map<String, ImageBasedDefaultCDHInfo> getEntries(Images images, String os, Architecture architecture) {
        return images.getCdhImages().stream()
                .filter(Image::isDefaultImage)
                .filter(image -> imageOsService.isSupported(image.getOs()))
                .filter(image -> ObjectUtils.isEmpty(os) || os.equalsIgnoreCase(image.getOs()))
                .filter(image -> architecture == null || Architecture.fromStringWithFallback(image.getArchitecture()) == architecture)
                .collect(Collectors.toMap(Image::getVersion, this::createImageBasedDefaultCDHInfo, preferOs()));
    }

    private ImageBasedDefaultCDHInfo createImageBasedDefaultCDHInfo(Image image) {
        DefaultCDHInfo defaultCdhInfo = new DefaultCDHInfo();
        defaultCdhInfo.setVersion(image.getStackDetails().getRepo().getStack().get(StackRepoDetails.REPOSITORY_VERSION));
        defaultCdhInfo.setRepo(getRepoDetails(image));
        defaultCdhInfo.setParcels(getParcels(image));
        defaultCdhInfo.setCsd(image.getPreWarmCsd());
        return new ImageBasedDefaultCDHInfo(defaultCdhInfo, image);
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
        Map<String, String> repoStack = new HashMap<>(image.getStackDetails().getRepo().getStack());
        repoStack.remove(StackRepoDetails.REPOSITORY_VERSION);
        repoDetails.setStack(repoStack);

        return repoDetails;
    }

    private BinaryOperator<ImageBasedDefaultCDHInfo> preferOs() {
        return (i1, i2) -> Stream.of(i1, i2)
                .filter(i -> imageOsService.getPreferredOs().equalsIgnoreCase(i.getImage().getOs()))
                .findFirst()
                .orElse(i1);
    }
}
