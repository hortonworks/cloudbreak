package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;

@Component
public class CsdLocationFilter implements PackageLocationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsdLocationFilter.class);

    @Override
    public boolean filterImage(Image image, Image currentImage, StackType stackType) {
        if (StackType.WORKLOAD.equals(stackType)) {
            if (image == null || image.getPreWarmCsd() == null || image.getPreWarmCsd().isEmpty()) {
                LOGGER.debug("Image or CSD parcels are not present. Image: {}", image);
                return false;
            } else {
                List<String> csdList = image.getPreWarmCsd();
                LOGGER.debug("Matching URLs: [{}]", csdList);
                return csdList.stream().allMatch(csdUrl -> URL_PATTERN.matcher(csdUrl).find());
            }
        } else {
            LOGGER.debug("Skip filtering because the stack type is {}", stackType);
            return false;
        }
    }
}
