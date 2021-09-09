package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;

@Component
public class ImageToClouderaManagerRepoConverter {

    public ClouderaManagerRepo convert(Image image) {
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setPredefined(Boolean.TRUE);
        clouderaManagerRepo.setVersion(image.getPackageVersion(ImagePackageVersion.CM));
        clouderaManagerRepo.setBaseUrl(image.getRepo().get(image.getOsType()));
        return clouderaManagerRepo;
    }
}
