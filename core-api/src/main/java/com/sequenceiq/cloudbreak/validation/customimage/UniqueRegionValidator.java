package com.sequenceiq.cloudbreak.validation.customimage;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request.CustomImageCatalogV4VmImageRequest;

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
