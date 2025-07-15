package com.sequenceiq.it.cloudbreak.util;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.ImageCatalog;

@Component
public class FreeIpaImageUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Pair<Image, Image> getLastUpgradeableImage(String catalogUrl, String cloudProvider, String region, Architecture architecture, boolean publishedOnly) {
        String body = RestClientUtil.get().target(catalogUrl).request().get().readEntity(String.class);
        try {
            ImageCatalog imageCatalog = objectMapper.readValue(body, ImageCatalog.class);
            List<Image> images = imageCatalog.getImages().getFreeipaImages().stream().sorted(Comparator.comparing(Image::getCreated).reversed()).toList();
            if (publishedOnly) {
                images = filterPublished(imageCatalog, images);
            }
            List<Image> lastTwoImages = images.stream()
                    .filter(image -> hasImageOnClouderProvider(image, cloudProvider, region) && hasArchitecture(image, architecture))
                    .limit(2)
                    .toList();
            if (lastTwoImages.size() != 2) {
                throw new RuntimeException(
                        String.format("Not found at least two %s freeipa images on %s (region: %s). " +
                                "Please burn more images in %s catalog.", architecture, cloudProvider, region, catalogUrl));
            }
            return Pair.of(lastTwoImages.get(1), lastTwoImages.get(0));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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
