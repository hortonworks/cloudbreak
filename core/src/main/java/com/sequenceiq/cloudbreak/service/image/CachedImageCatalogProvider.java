package com.sequenceiq.cloudbreak.service.image;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
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

@Component
public class CachedImageCatalogProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CachedImageCatalogProvider.class);

    @Value("${cb.etc.config.dir}")
    private String etcConfigDir;

    @Value("#{'${cb.enabled.linux.types}'.split(',')}")
    private List<String> enabledLinuxTypes;

    @Inject
    private ObjectMapper objectMapper;

    @Cacheable(cacheNames = "imageCatalogCache", key = "#catalogUrl")
    public CloudbreakImageCatalogV2 getImageCatalogV2(String catalogUrl) throws CloudbreakImageCatalogException {
        CloudbreakImageCatalogV2 catalog;
        if (catalogUrl == null) {
            LOGGER.info("No image catalog was defined!");
            return null;
        }

        try {
            long started = System.currentTimeMillis();
            String content;
            if (catalogUrl.startsWith("http")) {
                Client client = RestClientUtil.get();
                WebTarget target = client.target(catalogUrl);
                Response response = target.request().get();
                content = readResponse(target, response);
            } else {
                content = readCatalogFromFile(catalogUrl);
            }
            catalog = objectMapper.readValue(content, CloudbreakImageCatalogV2.class);
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
        List<Image> filteredCdhImages = filterImages(catalogImages.getCdhImages(), enabledOsPredicate());

        Images images = new Images(filteredBaseImages, filteredHdpImages, filteredHdfImages, filteredCdhImages, catalogImages.getSuppertedVersions());
        return new CloudbreakImageCatalogV2(images, catalog.getVersions());
    }

    private List<Image> filterImages(List<Image> imageList, Predicate<Image> predicate) {
        Map<Boolean, List<Image>> partitionedImages = Optional.ofNullable(imageList).orElse(Collections.emptyList()).stream()
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

    private String readResponse(WebTarget target, Response response) throws CloudbreakImageCatalogException {
        CloudbreakImageCatalogV2 catalog;
        if (!response.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
            throw new CloudbreakImageCatalogException(String.format("Failed to get image catalog from '%s' due to: '%s'",
                    target.getUri().toString(), response.getStatusInfo().getReasonPhrase()));
        }
        try {
            return response.readEntity(String.class);
        } catch (ProcessingException e) {
            throw new CloudbreakImageCatalogException(String.format("Failed to process image catalog from '%s' due to: '%s'",
                    target.getUri().toString(), e.getMessage()));
        }
    }

    @CacheEvict(value = "imageCatalogCache", key = "#catalogUrl")
    public void evictImageCatalogCache(String catalogUrl) {
    }

    private void validateImageCatalogUuids(CloudbreakImageCatalogV2 imageCatalog) throws CloudbreakImageCatalogException {
        Stream<String> baseUuids = imageCatalog.getImages().getBaseImages().stream().map(Image::getUuid);
        Stream<String> hdpUuids = imageCatalog.getImages().getHdpImages().stream().map(Image::getUuid);
        Stream<String> hdfUuids = imageCatalog.getImages().getHdfImages().stream().map(Image::getUuid);
        Stream<String> cdhUuids = imageCatalog.getImages().getCdhImages().stream().map(Image::getUuid);
        Stream<String> uuidStream = Stream.of(baseUuids, hdpUuids, hdfUuids, cdhUuids).
                reduce(Stream::concat).
                orElseGet(Stream::empty);
        List<String> uuidList = uuidStream.collect(Collectors.toList());
        List<String> orphanUuids = imageCatalog.getVersions().getCloudbreakVersions().stream().flatMap(cbv -> cbv.getImageIds().stream()).
                filter(imageId -> !uuidList.contains(imageId)).collect(Collectors.toList());
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
        boolean baseImagesValidate = cleanAndAllIsEmpty(catalog.getImages().getBaseImages());
        boolean hdfImagesValidate = cleanAndAllIsEmpty(catalog.getImages().getHdfImages());
        boolean hdpImagesValidate = cleanAndAllIsEmpty(catalog.getImages().getHdpImages());
        boolean cdhImagesValidate = cleanAndAllIsEmpty(catalog.getImages().getCdhImages());

        if (baseImagesValidate && hdfImagesValidate && hdpImagesValidate && cdhImagesValidate) {
            throw new CloudbreakImageCatalogException("All images are empty or every items equals NULL");
        }
    }

    private boolean cleanAndAllIsEmpty(List<Image> images) {
        return images.stream()
                .peek(i -> i.getImageSetsByProvider().values().removeIf(Objects::isNull))
                .allMatch(i -> i.getImageSetsByProvider().isEmpty());
    }

    private void validateCloudBreakVersions(CloudbreakImageCatalogV2 catalog) throws CloudbreakImageCatalogException {
        if (catalog.getVersions() == null || catalog.getVersions().getCloudbreakVersions().isEmpty()) {
            throw new CloudbreakImageCatalogException("Cloudbreak versions cannot be NULL");
        }
    }
}
