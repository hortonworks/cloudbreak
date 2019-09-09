package com.sequenceiq.freeipa.service.image;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameters;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.service.GetCloudParameterException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.converter.image.ImageToImageEntityConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.Image;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.ImageRepository;
import com.sequenceiq.freeipa.service.cloud.PlatformParameterService;

@Service
public class ImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageService.class);

    private static final String DEFAULT_REGION = "default";

    @Inject
    private UserDataBuilder userDataBuilder;

    @Inject
    private PlatformParameterService platformParameterService;

    @Inject
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Inject
    private ImageToImageEntityConverter imageConverter;

    @Inject
    private ImageRepository imageRepository;

    @Inject
    private ImageCatalogProvider imageCatalogProvider;

    @Value("${freeipa.image.catalog.url}")
    private String defaultCatalogUrl;

    @Value("${freeipa.image.catalog.default.os}")
    private String defaultOs;

    public Image create(Stack stack, ImageSettingsRequest imageRequest, Credential credential, CcmParameters ccmParameters) {
        Future<PlatformParameters> platformParametersFuture =
                intermediateBuilderExecutor.submit(() -> platformParameterService.getPlatformParameters(stack, credential));
        String userData = createUserData(stack, platformParametersFuture, ccmParameters);
        String region = stack.getRegion();
        String platformString = stack.getCloudPlatform().toLowerCase();
        com.sequenceiq.freeipa.api.model.image.Image imageCatalogImage = getImage(imageRequest, region, platformString);
        String imageName = determineImageName(platformString, region, imageCatalogImage);
        String catalogUrl = Objects.nonNull(imageRequest.getCatalog()) ? imageRequest.getCatalog() : defaultCatalogUrl;
        LOGGER.info("Selected VM image for CloudPlatform '{}' and region '{}' is: {} from: {} image catalog",
                platformString, region, imageName, catalogUrl);

        Image image = imageConverter.convert(imageCatalogImage);
        image.setStack(stack);
        image.setUserdata(userData);
        image.setImageName(imageName);
        image.setImageCatalogUrl(catalogUrl);
        return imageRepository.save(image);
    }

    public Image getByStack(Stack stack) {
        return imageRepository.getByStack(stack);
    }

    public com.sequenceiq.freeipa.api.model.image.Image getImage(ImageSettingsRequest imageSettings, String region, String platform) {
        String imageId = imageSettings.getId();
        String catalogUrl = Objects.nonNull(imageSettings.getCatalog()) ? imageSettings.getCatalog() : defaultCatalogUrl;
        String imageOs = Objects.nonNull(imageSettings.getOs()) ? imageSettings.getOs() : defaultOs;

        List<com.sequenceiq.freeipa.api.model.image.Image> images = imageCatalogProvider.getImageCatalog(catalogUrl).getImages().getFreeipaImages();
        Optional<? extends com.sequenceiq.freeipa.api.model.image.Image> image = findImage(imageId, imageOs, images, region, platform);
        if (image.isEmpty()) {
            imageCatalogProvider.evictImageCatalogCache(catalogUrl);
            images = imageCatalogProvider.getImageCatalog(catalogUrl).getImages().getFreeipaImages();
            image = findImage(imageId, imageOs, images, region, platform);
            if (image.isEmpty()) {
                throw new ImageNotFoundException(String.format("Could not find any image with id: '%s' in region '%s' with OS '%s'.", imageId, region, imageOs));
            }
        }
        return image.get();
    }

    public String determineImageName(String platformString, String region, com.sequenceiq.freeipa.api.model.image.Image imgFromCatalog) {
        Optional<Map<String, String>> imagesForPlatform = findStringKeyWithEqualsIgnoreCase(platformString, imgFromCatalog.getImageSetsByProvider());
        if (imagesForPlatform.isPresent()) {
            Map<String, String> imagesByRegion = imagesForPlatform.get();
            Optional<String> imageNameOpt = findStringKeyWithEqualsIgnoreCase(region, imagesByRegion);
            if (!imageNameOpt.isPresent()) {
                imageNameOpt = findStringKeyWithEqualsIgnoreCase(DEFAULT_REGION, imagesByRegion);
            }
            if (imageNameOpt.isPresent()) {
                return imageNameOpt.get();
            }
            String msg = String.format("Virtual machine image couldn't be found in image: '%s' for the selected platform: '%s' and region: '%s'.",
                    imgFromCatalog, platformString, region);
            throw new ImageNotFoundException(msg);
        }
        String msg = String.format("The selected image: '%s' doesn't contain virtual machine image for the selected platform: '%s'.",
                imgFromCatalog, platformString);
        throw new ImageNotFoundException(msg);
    }

    private List<com.sequenceiq.freeipa.api.model.image.Image> filterImages(List<com.sequenceiq.freeipa.api.model.image.Image> imageList, String os,
            String platform, String region) {
        Predicate<com.sequenceiq.freeipa.api.model.image.Image> predicate = img -> img.getOs().equalsIgnoreCase(os)
                && img.getImageSetsByProvider().containsKey(platform) && img.getImageSetsByProvider().get(platform).containsKey(region);
        Map<Boolean, List<com.sequenceiq.freeipa.api.model.image.Image>> partitionedImages =
                Optional.ofNullable(imageList).orElse(Collections.emptyList()).stream()
                .collect(Collectors.partitioningBy(predicate));
        if (hasFiltered(partitionedImages)) {
            LOGGER.debug("Used filter for: | {} | Images filtered: {}",
                    os,
                    partitionedImages.get(false).stream().map(com.sequenceiq.freeipa.api.model.image.Image::toString).collect(Collectors.joining(", ")));
            return partitionedImages.get(true);
        } else {
            LOGGER.warn("No FreeIPA image found with OS {}, falling back to the latest available one if such exists!", os);
            return imageList;
        }
    }

    private boolean hasFiltered(Map<Boolean, List<com.sequenceiq.freeipa.api.model.image.Image>> partitioned) {
        return !partitioned.get(true).isEmpty();
    }

    private Optional<? extends com.sequenceiq.freeipa.api.model.image.Image> findImage(String imageId, String imageOs,
            List<com.sequenceiq.freeipa.api.model.image.Image> images, String region, String platform) {
        if (Objects.nonNull(imageOs) && !imageOs.isEmpty()) {
            images = filterImages(images, imageOs, platform, region);
        }
        if (Objects.nonNull(imageId) && !imageId.isEmpty()) {
            return images.stream()
                    .filter(img -> img.getImageSetsByProvider().containsKey(platform) && img.getImageSetsByProvider().get(platform).containsKey(region))
                    .filter(img -> img.getImageSetsByProvider().get(platform).get(region).equalsIgnoreCase(imageId))
                    .max(Comparator.comparing(com.sequenceiq.freeipa.api.model.image.Image::getDate));
        } else {
            return images.stream().filter(image -> image.getImageSetsByProvider().containsKey(platform))
                    .max(Comparator.comparing(com.sequenceiq.freeipa.api.model.image.Image::getDate));
        }
    }

    private <T> Optional<T> findStringKeyWithEqualsIgnoreCase(String key, Map<String, T> map) {
        return map.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private String createUserData(Stack stack, Future<PlatformParameters> platformParametersFuture, CcmParameters ccmParameters) {
        SecurityConfig securityConfig = stack.getSecurityConfig();
        SaltSecurityConfig saltSecurityConfig = securityConfig.getSaltSecurityConfig();
        String cbPrivKey = saltSecurityConfig.getSaltBootSignPrivateKey();
        byte[] cbSshKeyDer = PkiUtil.getPublicKeyDer(new String(Base64.decodeBase64(cbPrivKey)));
        String sshUser = stack.getStackAuthentication().getLoginUserName();
        String cbCert = securityConfig.getClientCert();
        String saltBootPassword = saltSecurityConfig.getSaltBootPassword();
        PlatformParameters platformParameters = null;
        try {
            platformParameters = platformParametersFuture.get();
            return userDataBuilder.buildUserData(Platform.platform(stack.getCloudPlatform()), cbSshKeyDer, sshUser, platformParameters, saltBootPassword,
                    cbCert, ccmParameters);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Failed to get Platform parmaters", e);
            throw new GetCloudParameterException("Failed to get Platform parmaters", e);
        }
    }
}
