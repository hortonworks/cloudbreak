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

import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status.Family;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.FreeIpaVersions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.ImageCatalog;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Images;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Versions;

@Service
public class ImageCatalogProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogProvider.class);

    @Value("${cb.etc.config.dir}")
    private String etcConfigDir;

    @Value("#{'${cb.enabled.linux.types}'.split(',')}")
    private List<String> enabledLinuxTypes;

    @Value("${info.app.version:}")
    private String freeIpaVersion;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private RestClientFactory restClientFactory;

    @Cacheable(cacheNames = "imageCatalogCache", key = "#catalogUrl")
    public ImageCatalog getImageCatalog(String catalogUrl)  {
        try {
            if (Objects.nonNull(catalogUrl)) {
                long started = System.currentTimeMillis();
                String content = readCatalogContent(catalogUrl);
                ImageCatalog catalog = objectMapper.readValue(content, ImageCatalog.class);
                if (Objects.nonNull(catalog)) {
                    ImageCatalog filteredCatalog = filterImagesByOsType(catalog);
                    long timeOfParse = System.currentTimeMillis() - started;
                    LOGGER.debug("ImageCatalog was fetched and parsed from '{}' and took '{}' ms.", catalogUrl, timeOfParse);
                    return filteredCatalog;
                }
                throw new ImageCatalogException(String.format("Failed to read the content of '%s' as an image catalog.", catalogUrl));
            }
            throw new ImageCatalogException("Unable to fetch image catalog. The catalogUrl is null.");
        } catch (ImageCatalogException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ImageCatalogException(String.format("Failed to get image catalog: %s from %s", e.getCause(), catalogUrl), e);
        } catch (JsonMappingException e) {
            throw new ImageCatalogException(String.format("Invalid json format for image catalog with error: %s", e.getMessage()), e);
        } catch (IOException e) {
            throw new ImageCatalogException(String.format("Failed to read image catalog from file: '%s'", catalogUrl));
        }
    }

    @CacheEvict(value = "imageCatalogCache", key = "#catalogUrl")
    public void evictImageCatalogCache(String catalogUrl) {
    }

    private String readCatalogContent(String catalogUrl) throws IOException {
        if (catalogUrl.startsWith("http")) {
            Client client = restClientFactory.getOrCreateWithFollowRedirects();
            WebTarget target = client.target(catalogUrl);
            Response response = target.request().get();
            return readResponse(target, response);
        } else {
            return readCatalogFromFile(catalogUrl);
        }
    }

    private ImageCatalog filterImagesByOsType(ImageCatalog catalog) {
        LOGGER.debug("Filtering images by OS type {}", getEnabledLinuxTypes());
        if ((CollectionUtils.isEmpty(getEnabledLinuxTypes()) || Objects.isNull(catalog.getImages()))
                && Objects.nonNull(catalog.getVersions())) {
            return catalog;
        }
        List<Image> catalogImages = catalog.getImages().getFreeipaImages();
        List<Image> filterImages = filterImages(catalogImages, enabledOsPredicate());
        List<FreeIpaVersions> filteredVersions = filterVersions(catalog, filterImages);
        return new ImageCatalog(new Images(filterImages), new Versions(filteredVersions));
    }

    private List<FreeIpaVersions> filterVersions(ImageCatalog catalog, List<Image> filterImages) {
        List<String> filteredUuids = filterImages.stream().map(Image::getUuid).collect(Collectors.toList());
        LOGGER.debug("The following uuids will be removed from defaults and image ids fields: [{}]", filteredUuids);
        return getVersions(catalog).getFreeIpaVersions().stream()
                .map(versions -> filterDefaultsAndImageIds(filteredUuids, versions)).collect(Collectors.toList());
    }

    private FreeIpaVersions filterDefaultsAndImageIds(List<String> filteredUuids, FreeIpaVersions versions) {
        List<String> defaults = versions.getDefaults().stream().filter(filteredUuids::contains).collect(Collectors.toList());
        List<String> imageIds = versions.getImageIds().stream().filter(filteredUuids::contains).collect(Collectors.toList());
        LOGGER.debug("Filtered versions: [versions: {}, defaults: {}, images: {}]", versions.getVersions(), defaults, imageIds);
        return new FreeIpaVersions(versions.getVersions(), defaults, imageIds);
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
            throw new ImageCatalogException(String.format("Failed to get image catalog from '%s' due to: '%s'",
                    target.getUri().toString(), response.getStatusInfo().getReasonPhrase()));
        }
        try {
            return response.readEntity(String.class);
        } catch (ProcessingException e) {
            throw new ImageCatalogException(String.format("Failed to process image catalog from '%s' due to: '%s'",
                    target.getUri().toString(), e.getMessage()));
        }
    }

    private String readCatalogFromFile(String catalogUrl) throws IOException {
        File customCatalogFile = new File(etcConfigDir, catalogUrl);
        return FileReaderUtils.readFileFromPath(customCatalogFile.toPath());
    }

    private Versions getVersions(ImageCatalog catalog) {
        if (catalog.getVersions() == null || catalog.getVersions().getFreeIpaVersions() == null) {
            LOGGER.debug("FreeIPA versions are missing from the image catalog, generating it based on the current svc version and on advertised flags.");
            List<String> advertisedImageUuids = catalog.getImages().getFreeipaImages().stream()
                    .filter(Image::isAdvertised)
                    .map(Image::getUuid)
                    .collect(Collectors.toList());
            List<FreeIpaVersions> versionList = List.of(new FreeIpaVersions(List.of(freeIpaVersion), List.of(), advertisedImageUuids));
            Versions versions = new Versions(versionList);
            LOGGER.debug("Generated versions: '{}'", versions);
            return versions;
        } else {
            return catalog.getVersions();
        }
    }
}
