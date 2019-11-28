package com.sequenceiq.cloudbreak.service.image;

import static java.util.Collections.emptyList;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV2;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class CachedImageCatalogProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CachedImageCatalogProvider.class);

    @Value("${cb.etc.config.dir:}")
    private String etcConfigDir;

    @Value("#{'${cb.enabled.linux.types}'.split(',')}")
    private List<String> enabledLinuxTypes;

    @Inject
    private ObjectMapper objectMapper;

    @Cacheable(cacheNames = "imageCatalogCache", key = "#catalogUrl")
    public CloudbreakImageCatalogV2 getImageCatalogV2(String catalogUrl) throws CloudbreakImageCatalogException {
        CloudbreakImageCatalogV2 catalog;
        if (catalogUrl == null) {
            LOGGER.warn("No image catalog was defined!");
            return null;
        }

        try {
            long started = System.currentTimeMillis();
            if (catalogUrl.startsWith("http")) {
                Client client = RestClientUtil.get();
                WebTarget target = client.target(catalogUrl);
                Response response = target.request().get();
                catalog = checkResponse(target, response);
            } else {
                String content = readCatalogFromFile(catalogUrl);
                catalog = JsonUtil.readValue(content, CloudbreakImageCatalogV2.class);
            }
            validateImageCatalogUuids(catalog);
            validateCloudBreakVersions(catalog);
            cleanAndValidateMaps(catalog);
            catalog = filterImagesByOsType(catalog);
            long timeOfParse = System.currentTimeMillis() - started;
            LOGGER.debug("ImageCatalog has been get and parsed from '{}' and took '{}' ms.", catalogUrl, timeOfParse);
        } catch (RuntimeException e) {
            throw new CloudbreakImageCatalogException(String.format("Failed to get image catalog: %s from %s", e.getMessage(), catalogUrl), e);
        } catch (JsonMappingException e) {
            throw new CloudbreakImageCatalogException(e.getMessage(), e);
        } catch (IOException e) {
            throw new CloudbreakImageCatalogException(String.format("Failed to read image catalog from file: '%s'", catalogUrl), e);
        }
        return catalog;
    }

    private CloudbreakImageCatalogV2 filterImagesByOsType(CloudbreakImageCatalogV2 catalog) {
        LOGGER.debug("Filtering images by OS type {}", getEnabledLinuxTypes());
        if (CollectionUtils.isEmpty(getEnabledLinuxTypes()) || Objects.isNull(catalog) || Objects.isNull(catalog.getImages())) {
            return catalog;
        }

        Images catalogImages = catalog.getImages();

        List<Image> filteredBaseImages = filterImages(catalogImages.getBaseImages(), enabledOsPredicate());
        List<Image> filteredHdpImages = filterImages(catalogImages.getHdpImages(), enabledOsPredicate());
        List<Image> filteredHdfImages = filterImages(catalogImages.getHdfImages(), enabledOsPredicate());

        Images images = new Images(filteredBaseImages, filteredHdpImages, filteredHdfImages, emptyList(), catalogImages.getSuppertedVersions());
        return new CloudbreakImageCatalogV2(images, catalog.getVersions());
    }

    private List<Image> filterImages(List<Image> imageList, Predicate<Image> predicate) {
        Map<Boolean, List<Image>> partitionedImages = Optional.ofNullable(imageList).orElse(emptyList()).stream()
                .collect(Collectors.partitioningBy(predicate));
        if (hasFiltered(partitionedImages)) {
            LOGGER.debug("Used filter linuxTypes: | {} | Images filtered: {}",
                    getEnabledLinuxTypes(),
                    partitionedImages.get(false).stream().map(Image::shortOsDescriptionFormat).collect(Collectors.joining(", ")));
        }
        return partitionedImages.get(true);
    }

    private boolean hasFiltered(Map<Boolean, List<Image>> partitioned) {
        return !partitioned.get(false).isEmpty();
    }

    private Predicate<Image> enabledOsPredicate() {
        return img -> getEnabledLinuxTypes().stream().anyMatch(img.getOs()::equalsIgnoreCase);
    }

    private List<String> getEnabledLinuxTypes() {
        return enabledLinuxTypes.stream().filter(StringUtils::isNoneBlank).collect(Collectors.toList());
    }

    private CloudbreakImageCatalogV2 checkResponse(WebTarget target, Response response) throws CloudbreakImageCatalogException {
        CloudbreakImageCatalogV2 catalog;
        if (!response.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
            throw new CloudbreakImageCatalogException(String.format("Failed to get image catalog from '%s' due to: '%s'",
                    target.getUri().toString(), response.getStatusInfo().getReasonPhrase()));
        } else {
            try {
                String responseContent = response.readEntity(String.class);
                catalog = objectMapper.readValue(responseContent, CloudbreakImageCatalogV2.class);
            } catch (IOException | ProcessingException e) {
                throw new CloudbreakImageCatalogException(String.format("Failed to process image catalog from '%s' due to: '%s'",
                        target.getUri().toString(), e.getMessage()));
            }
        }
        return catalog;
    }

    @CacheEvict(value = "imageCatalogCache", key = "#catalogUrl")
    public void evictImageCatalogCache(String catalogUrl) {
    }

    private void validateImageCatalogUuids(CloudbreakImageCatalogV2 imageCatalog) throws CloudbreakImageCatalogException {
        Stream<String> uuidStream = Stream.concat(imageCatalog.getImages().getBaseImages().stream()
                        .map(Image::getUuid),
                imageCatalog.getImages().getHdpImages().stream().map(Image::getUuid));
        uuidStream = Stream.concat(uuidStream, imageCatalog.getImages().getHdfImages().stream().map(Image::getUuid));
        List<String> uuidList = uuidStream.collect(Collectors.toList());
        List<String> orphanUuids = imageCatalog.getVersions().getCloudbreakVersions().stream()
                .flatMap(cbv -> cbv.getImageIds().stream())
                .filter(imageId -> !uuidList.contains(imageId))
                .filter(imageId -> imageCatalog.getImages().getCdhImages().stream().noneMatch(it -> it.getUuid().equals(imageId)))
                .collect(Collectors.toList());
        if (!orphanUuids.isEmpty()) {
            throw new CloudbreakImageCatalogException(String.format("Images with ids: %s is not present in ambari-images block",
                    StringUtils.join(orphanUuids, ",")));
        }
    }

    private String readCatalogFromFile(String catalogUrl) throws IOException {
        File customCatalogFile = new File(etcConfigDir, catalogUrl);
        return FileReaderUtils.readFileFromPath(customCatalogFile.toPath());
    }

    private void cleanAndValidateMaps(CloudbreakImageCatalogV2 catalog) throws CloudbreakImageCatalogException {

        boolean baseImagesValidate = isCleanAndCheckMap(catalog.getImages().getBaseImages());
        boolean hdfImagesValidate = isCleanAndCheckMap(catalog.getImages().getHdfImages());
        boolean hdpImagesValidate = isCleanAndCheckMap(catalog.getImages().getHdpImages());

        if (baseImagesValidate && hdfImagesValidate && hdpImagesValidate) {
            throw new CloudbreakImageCatalogException("All images are empty or every items equals NULL");
        }


    }

    private boolean isCleanAndCheckMap(Collection<Image> images) {
        boolean valid = !images.isEmpty();
        if (valid) {
            for (Image image : images) {
                image.getImageSetsByProvider().values().removeIf(Objects::isNull);
                if (image.getImageSetsByProvider().isEmpty()) {
                    valid = false;
                }
            }
        }

        return !valid;
    }

    private void validateCloudBreakVersions(CloudbreakImageCatalogV2 catalog) throws CloudbreakImageCatalogException {
        if (catalog.getVersions() == null || catalog.getVersions().getCloudbreakVersions().isEmpty()) {
            throw new CloudbreakImageCatalogException("Cloudbreak versions cannot be NULL");
        }
    }
}
