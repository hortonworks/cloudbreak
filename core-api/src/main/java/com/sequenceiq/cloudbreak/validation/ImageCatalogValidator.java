package com.sequenceiq.cloudbreak.validation;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.validation.bean.ImageCatalogProvider;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class ImageCatalogValidator implements ConstraintValidator<ValidImageCatalog, String> {

    private static ImageCatalogProvider imageCatalogProvider;

    @Inject
    private ImageCatalogProvider imageCatalogProviderComponent;

    @PostConstruct
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public void init() {
        imageCatalogProvider = imageCatalogProviderComponent;
    }

    @Override
    public void initialize(ValidImageCatalog constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || !value.startsWith("http")) {
            return false;
        }
        try {
            imageCatalogProvider.getImageCatalogV2(value);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            imageCatalogProvider.evictImageCatalogCache(value);
        }
    }
}