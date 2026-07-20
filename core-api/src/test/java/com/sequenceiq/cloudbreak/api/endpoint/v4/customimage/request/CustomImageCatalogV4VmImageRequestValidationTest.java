package com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomImageCatalogV4VmImageRequestValidationTest {

    private static final String REGION_MESSAGE = "The region can only contain alphanumeric characters, spaces and hyphens";

    private static final String IMAGE_REFERENCE_MESSAGE =
            "The imageReference can only contain alphanumeric characters, spaces and the '-', '/', '_', '.' and ':' characters";

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void rejectsRegionContainingParentheses() {
        CustomImageCatalogV4VmImageRequest request = vmImage("(af-south-1)", "ami-0f89df708f139af1a");

        Set<ConstraintViolation<CustomImageCatalogV4VmImageRequest>> violations = validator.validateProperty(request, "region");

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo(REGION_MESSAGE);
    }

    @Test
    void rejectsRegionContainingDisallowedCharacter() {
        CustomImageCatalogV4VmImageRequest request = vmImage("af/south", "ami-0f89df708f139af1a");

        Set<ConstraintViolation<CustomImageCatalogV4VmImageRequest>> violations = validator.validateProperty(request, "region");

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo(REGION_MESSAGE);
    }

    @Test
    void rejectsImageReferenceContainingParentheses() {
        CustomImageCatalogV4VmImageRequest request = vmImage("af-south-1", "(ami-0f89df708f139af1a)");

        Set<ConstraintViolation<CustomImageCatalogV4VmImageRequest>> violations = validator.validateProperty(request, "imageReference");

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo(IMAGE_REFERENCE_MESSAGE);
    }

    @Test
    void rejectsImageReferenceContainingDisallowedCharacter() {
        CustomImageCatalogV4VmImageRequest request = vmImage("af-south-1", "ami#0f89df708f139af1a");

        Set<ConstraintViolation<CustomImageCatalogV4VmImageRequest>> violations = validator.validateProperty(request, "imageReference");

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo(IMAGE_REFERENCE_MESSAGE);
    }

    @Test
    void acceptsRegionWithHyphenAndImageReferenceWithAllowedSpecialCharacters() {
        CustomImageCatalogV4VmImageRequest request = vmImage("af-south-1", "cloudera:cdp/my_image-1.0:2");

        Set<ConstraintViolation<CustomImageCatalogV4VmImageRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void cascadesValidationIntoVmImagesOfCreateImageRequest() {
        CustomImageCatalogV4CreateImageRequest request = new CustomImageCatalogV4CreateImageRequest();
        request.setImageType("base");
        request.setSourceImageId("source-image-id");
        request.setVmImages(Set.of(vmImage("(af-south-1)", "ami-0f89df708f139af1a")));

        Set<ConstraintViolation<CustomImageCatalogV4CreateImageRequest>> violations = validator.validate(request);

        assertThat(getMessages(violations)).contains(REGION_MESSAGE);
    }

    @Test
    void cascadesValidationIntoVmImagesOfUpdateImageRequest() {
        CustomImageCatalogV4UpdateImageRequest request = new CustomImageCatalogV4UpdateImageRequest();
        request.setVmImages(Set.of(vmImage("af-south-1", "(ami-0f89df708f139af1a)")));

        Set<ConstraintViolation<CustomImageCatalogV4UpdateImageRequest>> violations = validator.validate(request);

        assertThat(getMessages(violations)).contains(IMAGE_REFERENCE_MESSAGE);
    }

    private static CustomImageCatalogV4VmImageRequest vmImage(String region, String imageReference) {
        CustomImageCatalogV4VmImageRequest request = new CustomImageCatalogV4VmImageRequest();
        request.setRegion(region);
        request.setImageReference(imageReference);
        return request;
    }

    private static List<String> getMessages(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toList());
    }
}
