package com.sequenceiq.sdx.validation;

import static com.sequenceiq.sdx.api.model.SdxUpgradeShowAvailableImages.DO_NOT_SHOW;
import static com.sequenceiq.sdx.api.model.SdxUpgradeShowAvailableImages.LATEST_ONLY;
import static com.sequenceiq.sdx.api.model.SdxUpgradeShowAvailableImages.SHOW;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.sdx.api.model.SdxUpgradeReplaceVms;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeShowAvailableImages;

@ExtendWith(MockitoExtension.class)
public class UpgradeRequestValidatorTest {

    private static final String IMAGE_ID = "36e85842-ea01-4cbc-6b1d-8f7a27fec49e";

    private static final String RUNTIME = "7.2.1";

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;

    @Mock
    private ConstraintValidatorContext validatorContext;

    @InjectMocks
    private UpgradeRequestValidator underTest;

    @ParameterizedTest
    @MethodSource("validUpgradeRequests")
    public void testValidRequests(String imageId, String runtime, Boolean lockComponents, Boolean dryRun,
            SdxUpgradeShowAvailableImages showAvailableImages, SdxUpgradeReplaceVms replaceVms) {
        SdxUpgradeRequest sdxUpgradeRequest = new SdxUpgradeRequest();
        sdxUpgradeRequest.setImageId(imageId);
        sdxUpgradeRequest.setRuntime(runtime);
        sdxUpgradeRequest.setLockComponents(lockComponents);
        sdxUpgradeRequest.setDryRun(dryRun);
        sdxUpgradeRequest.setShowAvailableImages(showAvailableImages);
        sdxUpgradeRequest.setReplaceVms(replaceVms);
        assertTrue(underTest.isValid(sdxUpgradeRequest, validatorContext));
    }

    @ParameterizedTest
    @MethodSource("invalidUpgradeRequests")
    public void testInvalidRequests(String imageId, String runtime, Boolean lockComponents, Boolean dryRun,
            SdxUpgradeShowAvailableImages showAvailableImages, SdxUpgradeReplaceVms replaceVms) {
        SdxUpgradeRequest sdxUpgradeRequest = new SdxUpgradeRequest();
        sdxUpgradeRequest.setImageId(imageId);
        sdxUpgradeRequest.setRuntime(runtime);
        sdxUpgradeRequest.setLockComponents(lockComponents);
        sdxUpgradeRequest.setDryRun(dryRun);
        sdxUpgradeRequest.setShowAvailableImages(showAvailableImages);
        sdxUpgradeRequest.setReplaceVms(replaceVms);
        assertFalse(underTest.isValid(sdxUpgradeRequest, validatorContext));
    }

    @ParameterizedTest
    @MethodSource("invalidUpgradeRequestsWithContext")
    public void testInvalidRequestsWithContext(String imageId, String runtime, Boolean lockComponents, Boolean dryRun,
            SdxUpgradeShowAvailableImages showAvailableImages, SdxUpgradeReplaceVms replaceVms) {

        when(validatorContext.buildConstraintViolationWithTemplate(any())).thenReturn(constraintViolationBuilder);
        when(constraintViolationBuilder.addConstraintViolation()).thenReturn(validatorContext);
        doNothing().when(validatorContext).disableDefaultConstraintViolation();

        SdxUpgradeRequest sdxUpgradeRequest = new SdxUpgradeRequest();
        sdxUpgradeRequest.setImageId(imageId);
        sdxUpgradeRequest.setRuntime(runtime);
        sdxUpgradeRequest.setLockComponents(lockComponents);
        sdxUpgradeRequest.setDryRun(dryRun);
        sdxUpgradeRequest.setShowAvailableImages(showAvailableImages);
        sdxUpgradeRequest.setReplaceVms(replaceVms);
        assertFalse(underTest.isValid(sdxUpgradeRequest, validatorContext));
    }

    static Stream<Arguments> validUpgradeRequests() {
        return Stream.of(
                Arguments.of(IMAGE_ID, null, null, null, null, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(IMAGE_ID, null, null, null, null, SdxUpgradeReplaceVms.ENABLED),
                Arguments.of(null, RUNTIME, null, null, null, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(null, RUNTIME, null, null, null, SdxUpgradeReplaceVms.ENABLED),
                Arguments.of(null, null, true, null, null, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(null, null, true, null, null, SdxUpgradeReplaceVms.ENABLED),
                // dry-run-s
                Arguments.of(IMAGE_ID, null, null, true, null, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(IMAGE_ID, null, null, true, null, SdxUpgradeReplaceVms.ENABLED),
                Arguments.of(null, RUNTIME, null, true, null, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(null, RUNTIME, null, true, null, SdxUpgradeReplaceVms.ENABLED),
                Arguments.of(null, null, true, true, null, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(null, null, true, true, null, SdxUpgradeReplaceVms.ENABLED),
                // SHOW
                Arguments.of(IMAGE_ID, null, null, null, SHOW, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(IMAGE_ID, null, null, null, SHOW, SdxUpgradeReplaceVms.ENABLED),
                Arguments.of(null, RUNTIME, null, null, SHOW, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(null, RUNTIME, null, null, SHOW, SdxUpgradeReplaceVms.ENABLED),
                Arguments.of(null, null, true, null, SHOW, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(null, null, true, null, SHOW, SdxUpgradeReplaceVms.ENABLED),
                // DO_NOT_SHOW
                Arguments.of(IMAGE_ID, null, null, null, DO_NOT_SHOW, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(IMAGE_ID, null, null, null, DO_NOT_SHOW, SdxUpgradeReplaceVms.ENABLED),
                Arguments.of(null, RUNTIME, null, null, DO_NOT_SHOW, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(null, RUNTIME, null, null, DO_NOT_SHOW, SdxUpgradeReplaceVms.ENABLED),
                Arguments.of(null, null, true, null, DO_NOT_SHOW, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(null, null, true, null, DO_NOT_SHOW, SdxUpgradeReplaceVms.ENABLED),
                // LATEST_ONLY
                Arguments.of(IMAGE_ID, null, null, null, LATEST_ONLY, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(IMAGE_ID, null, null, null, LATEST_ONLY, SdxUpgradeReplaceVms.ENABLED),
                Arguments.of(null, RUNTIME, null, null, LATEST_ONLY, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(null, RUNTIME, null, null, LATEST_ONLY, SdxUpgradeReplaceVms.ENABLED),
                Arguments.of(null, null, true, null, LATEST_ONLY, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(null, null, true, null, LATEST_ONLY, SdxUpgradeReplaceVms.ENABLED),
                // DO_NOT_SHOW + dry-run
                Arguments.of(IMAGE_ID, null, null, true, DO_NOT_SHOW, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(IMAGE_ID, null, null, true, DO_NOT_SHOW, SdxUpgradeReplaceVms.ENABLED),
                Arguments.of(null, RUNTIME, null, true, DO_NOT_SHOW, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(null, RUNTIME, null, true, DO_NOT_SHOW, SdxUpgradeReplaceVms.ENABLED),
                Arguments.of(null, null, true, true, DO_NOT_SHOW, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(null, null, true, true, DO_NOT_SHOW, SdxUpgradeReplaceVms.ENABLED)
        );
    }

    static Stream<Arguments> invalidUpgradeRequests() {
        return Stream.of(
                Arguments.of(IMAGE_ID, RUNTIME, null, null, null, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(IMAGE_ID, RUNTIME, true, null, null, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(null, RUNTIME, true, null, null, SdxUpgradeReplaceVms.DISABLED)
        );
    }

    static Stream<Arguments> invalidUpgradeRequestsWithContext() {
        return Stream.of(
                // SHOW + dry-run
                Arguments.of(IMAGE_ID, null, null, true, SHOW, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(IMAGE_ID, null, null, true, SHOW, SdxUpgradeReplaceVms.ENABLED),
                Arguments.of(null, RUNTIME, null, true, SHOW, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(null, RUNTIME, null, true, SHOW, SdxUpgradeReplaceVms.ENABLED),
                Arguments.of(null, null, true, true, SHOW, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(null, null, true, true, SHOW, SdxUpgradeReplaceVms.ENABLED),
                // LATEST_ONLY + dry-run
                Arguments.of(IMAGE_ID, null, null, true, LATEST_ONLY, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(IMAGE_ID, null, null, true, LATEST_ONLY, SdxUpgradeReplaceVms.ENABLED),
                Arguments.of(null, RUNTIME, null, true, LATEST_ONLY, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(null, RUNTIME, null, true, LATEST_ONLY, SdxUpgradeReplaceVms.ENABLED),
                Arguments.of(null, null, true, true, LATEST_ONLY, SdxUpgradeReplaceVms.DISABLED),
                Arguments.of(null, null, true, true, LATEST_ONLY, SdxUpgradeReplaceVms.ENABLED)
        );
    }
}
