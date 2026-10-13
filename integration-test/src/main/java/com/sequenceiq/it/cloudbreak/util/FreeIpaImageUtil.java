package com.sequenceiq.it.cloudbreak.util;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.ImageCatalog;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

@Component
public class FreeIpaImageUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaImageUtil.class);

    @Inject
    private ImageCatalogDownloader imageCatalogDownloader;

    public Pair<Image, Image> getLastUpgradeableImage(String catalogUrl, String cloudProvider, String region, Architecture architecture, boolean publishedOnly) {
        ImageCatalog imageCatalog = imageCatalogDownloader.download(catalogUrl, ImageCatalog.class);
        List<Image> images = imageCatalog.getImages().getFreeipaImages().stream().sorted(Comparator.comparing(Image::getCreated).reversed()).toList();
        if (publishedOnly) {
            images = filterPublished(imageCatalog, images);
        }
        List<Image> lastTwoImages = images.stream()
                .filter(image -> hasImageOnClouderProvider(image, cloudProvider, region) && hasArchitecture(image, architecture))
                .limit(2)
                .sorted((o1, o2) -> {
                    OsType o1Os = OsType.getByOs(o1.getOs());
                    OsType o2Os = OsType.getByOs(o2.getOs());
                    return Integer.compare(o2Os.ordinal(), o1Os.ordinal());
                })
                .toList();
        if (lastTwoImages.size() != 2) {
            throw new RuntimeException(
                    String.format("Not found at least two %s freeipa images on %s (region: %s). " +
                            "Please burn more images in %s catalog.", architecture, cloudProvider, region, catalogUrl));
        }
        return Pair.of(lastTwoImages.get(1), lastTwoImages.get(0));
    }

    public LastAndNewestImage getLastAndNewestImageByOs(String catalogUrl, Architecture architecture,
            String cloudProvider, OsType sourceOS, OsType destinationOS) {
        ImageCatalog imageCatalog = imageCatalogDownloader.download(catalogUrl, ImageCatalog.class);
        List<Image> providerImages = imageCatalog.getImages().getFreeipaImages().stream()
                .filter(image -> hasImageOnCloudProvider(image, cloudProvider) && hasArchitecture(image, architecture))
                .sorted(Comparator.comparing(Image::getDate).reversed())
                .toList();
        if (sourceOS.getOs().equalsIgnoreCase(destinationOS.getOs())) {
            List<Image> sameOsImages = providerImages.stream()
                    .filter(image -> sourceOS.getOs().equalsIgnoreCase(image.getOs()))
                    .limit(2)
                    .toList();
            if (sameOsImages.size() != 2) {
                throw new RuntimeException(String.format(
                        "Not found at least two '%s' freeipa images on %s. Please burn more images in %s catalog.",
                        sourceOS, cloudProvider, catalogUrl));
            }
            return new LastAndNewestImage(sameOsImages.get(1), sameOsImages.get(0));
        }
        Image last = newestImageByOs(providerImages, sourceOS.getOs(), cloudProvider, catalogUrl);
        Image newest = newestImageByOs(providerImages, destinationOS.getOs(), cloudProvider, catalogUrl);
        return new LastAndNewestImage(last, newest);
    }

    public String getNewestImageId(String catalogUrl, String cloudProvider, String region, String os, Architecture architecture) {
        ImageCatalog imageCatalog = imageCatalogDownloader.download(catalogUrl, ImageCatalog.class);
        Image newestImage = imageCatalog.getImages().getFreeipaImages().stream()
                .filter(image -> hasImageOnClouderProvider(image, cloudProvider, region) && hasArchitecture(image, architecture))
                .filter(image -> os.equalsIgnoreCase(image.getOs()))
                .max(Comparator.comparing(Image::getCreated))
                .orElseThrow(() -> new TestFailException(String.format(
                        "Not found any '%s' %s freeipa image on %s (region: %s). Please burn an image in %s catalog.",
                        os, architecture, cloudProvider, region, catalogUrl)));
        Log.log(LOGGER, " Selected newest '%s' FreeIPA image on %s (region: %s) from %s catalog | ID: %s | Date: %s ",
                os, cloudProvider, region, catalogUrl, newestImage.getUuid(), newestImage.getDate());
        return newestImage.getUuid();
    }

    private Image newestImageByOs(List<Image> providerImages, String os, String cloudProvider, String catalogUrl) {
        return providerImages.stream()
                .filter(image -> os.equalsIgnoreCase(image.getOs()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(String.format(
                        "Not found any '%s' freeipa image on %s. Please burn an image in %s catalog.",
                        os, cloudProvider, catalogUrl)));
    }

    private boolean hasImageOnCloudProvider(Image image, String cloudProvider) {
        return image.getImageSetsByProvider().containsKey(cloudProvider.toLowerCase());
    }

    private List<Image> filterPublished(ImageCatalog imageCatalog, List<Image> images) {
        Set<String> publishedImageIds = imageCatalog.getVersions().getFreeIpaVersions().stream()
                .flatMap(freeIpaVersions -> freeIpaVersions.getImageIds().stream())
                .collect(Collectors.toSet());
        return images.stream()
                .filter(image -> publishedImageIds.contains(image.getUuid()))
                .toList();
    }

    private boolean hasImageOnClouderProvider(Image image, String cloudProvider, String region) {
        return image.getImageSetsByProvider().containsKey(cloudProvider.toLowerCase())
                && image.getImageSetsByProvider().get(cloudProvider.toLowerCase()).containsKey(region);
    }

    private boolean hasArchitecture(Image image, Architecture architecture) {
        return Optional.ofNullable(architecture).orElse(Architecture.X86_64).equals(Architecture.fromStringWithFallback(image.getArchitecture()));
    }
}
