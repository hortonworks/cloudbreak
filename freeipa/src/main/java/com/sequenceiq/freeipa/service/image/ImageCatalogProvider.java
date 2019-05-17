package com.sequenceiq.freeipa.service.image;

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
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.freeipa.api.model.image.Image;
import com.sequenceiq.freeipa.api.model.image.ImageCatalog;

@Service
public class ImageCatalogProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogProvider.class);

    @Value("${cb.etc.config.dir}")
    private String etcConfigDir;

    @Value("#{'${cb.enabled.linux.types}'.split(',')}")
    private List<String> enabledLinuxTypes;

    @Value("${cb.image.catalog.url}")
    private String defaultCatalogUrl;

    @Value("${cb.image.catalog.default.os}")
    private String defaultOs;

    @Inject
    private ObjectMapper objectMapper;

    @Cacheable(cacheNames = "imageCatalogCache", key = "#catalogUrl")
    public ImageCatalog getImageCatalog(String catalogUrl)  {
        ImageCatalog catalog;
        if (StringUtils.isEmpty(catalogUrl)) {
            LOGGER.info("No image catalog was defined! trying for default");

            if (StringUtils.isEmpty(defaultCatalogUrl)) {
                LOGGER.info("No image catalog was defined! no default found");
                return null;
            }
            catalogUrl = defaultCatalogUrl;
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
            JsonNode images = objectMapper.readTree(content);
            JsonNode freeIpaImages = images.at("/images/freeipa-images");
            catalog = objectMapper.readValue(objectMapper.treeAsTokens(freeIpaImages), new TypeReference<ImageCatalog>() { });
            catalog = filterImagesByOsType(catalog);
            long timeOfParse = System.currentTimeMillis() - started;
            LOGGER.debug("ImageCatalog was fetched and parsed from '{}' and took '{}' ms.", catalogUrl, timeOfParse);
        } catch (RuntimeException e) {
            throw new RuntimeException(String.format("Failed to get image catalog: %s from %s", e.getCause(), catalogUrl), e);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to read image catalog from file: '%s'", catalogUrl), e);
        }
        return catalog;
    }

    @CacheEvict(value = "imageCatalogCache", key = "#catalogUrl")
    public void evictImageCatalogCache(String catalogUrl) {
    }

    private ImageCatalog filterImagesByOsType(ImageCatalog catalog) {
        LOGGER.debug("Filtering images by OS type {}", getEnabledLinuxTypes());
        if (CollectionUtils.isEmpty(getEnabledLinuxTypes()) || Objects.isNull(catalog) || Objects.isNull(catalog.getImages())) {
            return catalog;
        }
        List<Image> catalogImages = catalog.getImages();
        List<Image> filterImages = filterImages(catalogImages, enabledOsPredicate());
        return new ImageCatalog(filterImages);
    }

    private List<Image> filterImages(List<Image> imageList, Predicate<Image> predicate) {
        Map<Boolean, List<Image>> partitionedImages = Optional.ofNullable(imageList).orElse(Collections.emptyList()).stream()
                .collect(Collectors.partitioningBy(predicate));
        if (hasFiltered(partitionedImages)) {
            LOGGER.debug("Used filter linuxTypes: | {} | Images filtered: {}",
                    getEnabledLinuxTypes(),
                    partitionedImages.get(false).stream().map(Image::toString).collect(Collectors.joining(", ")));
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

    private String readResponse(WebTarget target, Response response)  {
        if (!response.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
            throw new RuntimeException(String.format("Failed to get image catalog from '%s' due to: '%s'",
                    target.getUri().toString(), response.getStatusInfo().getReasonPhrase()));
        }
        try {
            return response.readEntity(String.class);
        } catch (ProcessingException e) {
            throw new RuntimeException(String.format("Failed to process image catalog from '%s' due to: '%s'",
                    target.getUri().toString(), e.getMessage()));
        }
    }

    private String readCatalogFromFile(String catalogUrl) throws IOException {
        File customCatalogFile = new File(etcConfigDir, catalogUrl);
        return FileReaderUtils.readFileFromPath(customCatalogFile.toPath());
    }
}
