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
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.catalog.ImageCatalogServiceProxy;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class CachedImageCatalogProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CachedImageCatalogProvider.class);

    @Value("${cb.etc.config.dir:}")
    private String etcConfigDir;

    @Value("#{'${cb.enabled.linux.types}'.split(',')}")
    private List<String> enabledLinuxTypes;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private ImageCatalogServiceProxy imageCatalogServiceProxy;

    @Cacheable(cacheNames = "imageCatalogCache", key = "#catalogUrl")
    public CloudbreakImageCatalogV3 getImageCatalogV3(String catalogUrl) throws CloudbreakImageCatalogException {
        CloudbreakImageCatalogV3 catalog;
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
            catalog = objectMapper.readValue(content, CloudbreakImageCatalogV3.class);
            imageCatalogServiceProxy.validate(catalog);
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

    private CloudbreakImageCatalogV3 filterImagesByOsType(CloudbreakImageCatalogV3 catalog) {
        LOGGER.debug("Filtering images by OS type {}", getEnabledLinuxTypes());
        if (CollectionUtils.isEmpty(getEnabledLinuxTypes()) || Objects.isNull(catalog) || Objects.isNull(catalog.getImages())) {
            return catalog;
        }

        Images catalogImages = catalog.getImages();

        List<Image> filteredBaseImages = filterImages(catalogImages.getBaseImages(), enabledOsPredicate());
        List<Image> filteredCdhImages = filterImages(catalogImages.getCdhImages(), enabledOsPredicate());
        List<Image> filteredFreeipaImages = filterImages(catalogImages.getFreeIpaImages(), enabledOsPredicate());

        Images images = new Images(filteredBaseImages, filteredCdhImages, filteredFreeipaImages, catalogImages.getSuppertedVersions());
        return new CloudbreakImageCatalogV3(images, catalog.getVersions());
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
        CloudbreakImageCatalogV3 catalog;
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

    private String readCatalogFromFile(String catalogUrl) throws IOException {
        File customCatalogFile = new File(etcConfigDir, catalogUrl);
        return FileReaderUtils.readFileFromPath(customCatalogFile.toPath());
    }

    private void cleanAndValidateMaps(CloudbreakImageCatalogV3 catalog) throws CloudbreakImageCatalogException {
        boolean baseImagesValidate = cleanAndAllIsEmpty(catalog.getImages().getBaseImages());
        boolean cdhImagesValidate = cleanAndAllIsEmpty(catalog.getImages().getCdhImages());
        boolean freeipaImagesValidate = cleanAndAllIsEmpty(catalog.getImages().getFreeIpaImages());

        if (!catalog.getImages().getFreeIpaImages().isEmpty()) {
            if (freeipaImagesValidate) {
                throw new CloudbreakImageCatalogException("Freeipa images are empty or every items equals NULL");
            }
        } else {
            if (baseImagesValidate && cdhImagesValidate) {
                throw new CloudbreakImageCatalogException("Base and CDH images are empty or every items equals NULL");
            }
        }
    }

    private boolean cleanAndAllIsEmpty(List<Image> images) {
        return images.stream()
                .peek(i -> i.getImageSetsByProvider().values().removeIf(Objects::isNull))
                .allMatch(i -> i.getImageSetsByProvider().isEmpty());
    }
}
