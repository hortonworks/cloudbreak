package com.sequenceiq.cloudbreak.converter.v4.imagecatalog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseStackDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ClouderaManagerStackDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ClouderaManagerStackRepoDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerRepositoryV4Response;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.service.DefaultClouderaManagerRepoService;

@Component
public class ImagesToImagesV4ResponseConverter extends AbstractConversionServiceAwareConverter<Images, ImagesV4Response> {

    @Inject
    private DefaultCDHEntries defaultCDHEntries;

    @Inject
    private DefaultClouderaManagerRepoService defaultClouderaManagerRepoService;

    @Override
    public ImagesV4Response convert(Images source) {
        ImagesV4Response res = new ImagesV4Response();
        List<BaseImageV4Response> baseImages = getBaseImageResponses(source);
        res.setBaseImages(baseImages);

        List<ImageV4Response> hdpImages = convertImages(source.getHdpImages(), StackType.HDP);
        List<ImageV4Response> hdfImages = convertImages(source.getHdfImages(), StackType.HDF);
        List<ImageV4Response> cdhImages = convertImages(source.getCdhImages(), StackType.CDH);

        res.setHdpImages(hdpImages);
        res.setHdfImages(hdfImages);
        res.setCdhImages(cdhImages);
        res.setSupportedVersions(source.getSuppertedVersions());
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
        List<ClouderaManagerStackDetailsV4Response> defaultCdhStacks = getDefaultCdhStackInfo(defaultCDHEntries.getEntries().values());
        List<BaseImageV4Response> baseImages = source.getBaseImages().stream()
                .filter(image -> defaultClouderaManagerRepoService.getDefault(image.getOsType()) != null)
                .map(image -> {
                    BaseImageV4Response imgJson = new BaseImageV4Response();
                    copyImageFieldsToJson(image, imgJson);
                    imgJson.setCdhStacks(defaultCdhStacks);
                    ClouderaManagerRepo clouderaManagerRepo = defaultClouderaManagerRepoService.getDefault(image.getOsType());
                    if (clouderaManagerRepo != null) {
                        ClouderaManagerRepositoryV4Response clouderaManagerRepoJson = new ClouderaManagerRepositoryV4Response();
                        clouderaManagerRepoJson.setBaseUrl(clouderaManagerRepo.getBaseUrl());
                        clouderaManagerRepoJson.setVersion(clouderaManagerRepo.getVersion());
                        clouderaManagerRepoJson.setGpgKeyUrl(clouderaManagerRepo.getGpgKeyUrl());
                        imgJson.setClouderaManagerRepo(clouderaManagerRepoJson);
                    }
                    Map<String, String> repoJson = new HashMap<>();
                    imgJson.setRepository(repoJson);
                    return imgJson;
                })
                .collect(Collectors.toList());
        return baseImages;
    }

    private List<ClouderaManagerStackDetailsV4Response> getDefaultCdhStackInfo(Collection<DefaultCDHInfo> defaultStackInfo) {
        List<ClouderaManagerStackDetailsV4Response> result = new ArrayList<>();
        for (DefaultCDHInfo info : defaultStackInfo) {
            ClouderaManagerStackDetailsV4Response json = new ClouderaManagerStackDetailsV4Response();
            ClouderaManagerStackRepoDetailsV4Response repoJson = new ClouderaManagerStackRepoDetailsV4Response();
            Map<String, String> stackRepo = info.getRepo().getStack();
            if (stackRepo != null) {
                repoJson.setStack(stackRepo);
            }
            json.setRepository(repoJson);
            json.setVersion(info.getVersion());
            result.add(json);
        }
        return result;
    }

    private void copyImageFieldsToJson(Image source, ImageV4Response json) {
        json.setDate(source.getDate());
        json.setCreated(source.getCreated());
        json.setDescription(source.getDescription());
        json.setOs(source.getOs());
        json.setOsType(source.getOsType());
        json.setUuid(source.getUuid());
        json.setVersion(source.getVersion());
        json.setDefaultImage(source.isDefaultImage());
        json.setPackageVersions(source.getPackageVersions());
        if (source.getRepo() != null) {
            json.setRepository(new HashMap<>(source.getRepo()));
        } else {
            json.setRepository(new HashMap<>());
        }
        json.setImageSetsByProvider(new HashMap<>(source.getImageSetsByProvider()));
        json.setCmBuildNumber(source.getCmBuildNumber());
    }

    private BaseStackDetailsV4Response convertStackDetailsToJson(StackDetails stackDetails, String osType, StackType stackType) {
        if (StackType.CDH.equals(stackType)) {
            return convertClouderaManagerStackDetailsToJson(stackDetails);
        }
        return null;
    }

    private ClouderaManagerStackDetailsV4Response convertClouderaManagerStackDetailsToJson(StackDetails stackDetails) {
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
