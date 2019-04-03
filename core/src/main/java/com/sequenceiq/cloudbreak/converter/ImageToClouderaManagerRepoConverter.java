package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@Component
public class ImageToClouderaManagerRepoConverter extends AbstractConversionServiceAwareConverter<Image, ClouderaManagerRepo> {

    @Override
    public ClouderaManagerRepo convert(Image image) {
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setPredefined(Boolean.TRUE);
        clouderaManagerRepo.setVersion(image.getVersion());
        clouderaManagerRepo.setBaseUrl(image.getRepo().get(image.getOsType()));
        return clouderaManagerRepo;
    }
}
