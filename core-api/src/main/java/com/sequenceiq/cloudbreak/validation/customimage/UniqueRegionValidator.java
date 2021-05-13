package com.sequenceiq.cloudbreak.validation.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request.CustomImageCatalogV4VmImageRequest;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;

public class UniqueRegionValidator implements ConstraintValidator<UniqueRegion, Set<CustomImageCatalogV4VmImageRequest>> {

    @Override
    public boolean isValid(Set<CustomImageCatalogV4VmImageRequest> value, ConstraintValidatorContext context) {
        if (value != null) {
            Set<String> regions = new HashSet<>();
            for (CustomImageCatalogV4VmImageRequest vmImage : value) {
                if (!regions.add(vmImage.getRegion())) {
                    return false;
                }
            }
        }
        return true;
    }
}
