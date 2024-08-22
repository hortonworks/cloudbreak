package com.sequenceiq.cloudbreak.converter.v4.imagecatalog;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseStackDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ClouderaManagerStackDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ClouderaManagerStackRepoDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.ImageBasedDefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.ImageBasedDefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;

@Component
public class ImagesToImagesV4ResponseConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImagesToImagesV4ResponseConverter.class);

    @Inject
    private ImageBasedDefaultCDHEntries imageBasedDefaultCDHEntries;

    @Inject
    private ImageToImageV4ResponseConverter imageToImageV4ResponseConverter;

    public ImagesV4Response convert(Images source) {
        ImagesV4Response res = new ImagesV4Response();
        List<BaseImageV4Response> baseImages = getBaseImageResponses(source);
        res.setBaseImages(baseImages);
        List<ImageV4Response> cdhImages = convertImages(source.getCdhImages(), StackType.CDH);
        res.setCdhImages(cdhImages);
        res.setSupportedVersions(source.getSupportedVersions());
        res.setFreeipaImages(source.getFreeIpaImages().stream()
                .map(image -> imageToImageV4ResponseConverter.convert(image))
                .collect(Collectors.toList()));
        return res;
    }

    private List<ImageV4Response> convertImages(List<Image> source, StackType stackType) {
        return source.stream().map(img -> {
            ImageV4Response imgJson = new ImageV4Response();
            copyImageFieldsToJson(img, imgJson);
            imgJson.setStackDetails(convertStackDetailsToJson(img.getStackDetails(), img.getOsType(), stackType));
            return imgJson;
        }).collect(Collectors.toList());
    }

    private List<BaseImageV4Response> getBaseImageResponses(Images source) {
        Map<String, ImageBasedDefaultCDHInfo> imageBasedDefaultCDHInfoMap = imageBasedDefaultCDHEntries.getEntries(source);
        List<ClouderaManagerStackDetailsV4Response> defaultCdhStacks = getDefaultCdhStackInfo(imageBasedDefaultCDHInfoMap);
        List<BaseImageV4Response> baseImages = source.getBaseImages().stream()
                .map(image -> {
                    BaseImageV4Response imgJson = new BaseImageV4Response();
                    copyImageFieldsToJson(image, imgJson);
                    imgJson.setCdhStacks(defaultCdhStacks);
                    imgJson.setRepository(new HashMap<>());
                    return imgJson;
                })
                .sorted((o1, o2) -> parseDate(o1).compareTo(parseDate(o2)))
                .collect(Collectors.toList());
        return baseImages;
    }

    private Date parseDate(BaseImageV4Response image) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        try {
            return formatter.parse(image.getDate());
        } catch (ParseException e) {
            LOGGER.error("Invalid Date in image catalog for image: {}", image);
            return new Date();
        }
    }

    private List<ClouderaManagerStackDetailsV4Response> getDefaultCdhStackInfo(Map<String, ImageBasedDefaultCDHInfo> defaultStackInfo) {
        List<ClouderaManagerStackDetailsV4Response> result = new ArrayList<>();
        for (ImageBasedDefaultCDHInfo info : defaultStackInfo.values()) {
            ClouderaManagerStackDetailsV4Response json = new ClouderaManagerStackDetailsV4Response();
            ClouderaManagerStackRepoDetailsV4Response repoJson = new ClouderaManagerStackRepoDetailsV4Response();
            Map<String, String> stackRepo = info.getDefaultCDHInfo().getRepo().getStack();
            if (stackRepo != null) {
                repoJson.setStack(stackRepo);
            }
            json.setRepository(repoJson);
            json.setVersion(info.getDefaultCDHInfo().getVersion());
            result.add(json);
        }
        return result;
    }

    private void copyImageFieldsToJson(Image source, ImageV4Response json) {
        json.setDate(source.getDate());
        json.setCreated(source.getCreated());
        json.setPublished(source.getPublished());
        json.setDescription(source.getDescription());
        json.setOs(source.getOs());
        json.setOsType(source.getOsType());
        json.setArchitecture(source.getArchitecture());
        json.setUuid(source.getUuid());
        json.setVersion(source.getVersion());
        json.setDefaultImage(source.isDefaultImage());
        json.setPackageVersions(source.getPackageVersions());
        json.setAdvertised(source.isAdvertised());
        if (source.getRepo() != null) {
            json.setRepository(new HashMap<>(source.getRepo()));
        } else {
            json.setRepository(new HashMap<>());
        }
        json.setImageSetsByProvider(new HashMap<>(source.getImageSetsByProvider()));
        json.setCmBuildNumber(source.getCmBuildNumber());
        json.setPreWarmCsd(source.getPreWarmCsd());
        json.setPreWarmParcels(source.getPreWarmParcels());
    }

    private BaseStackDetailsV4Response convertStackDetailsToJson(ImageStackDetails stackDetails, String osType, StackType stackType) {
        if (StackType.CDH.equals(stackType)) {
            return convertClouderaManagerStackDetailsToJson(stackDetails);
        }
        return null;
    }

    private ClouderaManagerStackDetailsV4Response convertClouderaManagerStackDetailsToJson(ImageStackDetails stackDetails) {
        ClouderaManagerStackDetailsV4Response json = new ClouderaManagerStackDetailsV4Response();
        json.setVersion(stackDetails.getVersion());
        json.setRepository(convertClouderaManagerStackRepoDetailsToJson(stackDetails.getRepo()));
        json.setStackBuildNumber(stackDetails.getStackBuildNumber());
        return json;
    }

    private ClouderaManagerStackRepoDetailsV4Response convertClouderaManagerStackRepoDetailsToJson(StackRepoDetails repo) {
        ClouderaManagerStackRepoDetailsV4Response json = new ClouderaManagerStackRepoDetailsV4Response();
        json.setStack(new HashMap<>(repo.getStack()));
        return json;
    }
}
