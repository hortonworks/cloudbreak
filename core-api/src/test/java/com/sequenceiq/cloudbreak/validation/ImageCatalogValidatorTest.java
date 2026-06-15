package com.sequenceiq.cloudbreak.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.ws.rs.core.Response.Status.Family;
import jakarta.ws.rs.core.Response.StatusType;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.helper.HttpHelper;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@ExtendWith(MockitoExtension.class)
class ImageCatalogValidatorTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:accountId:user:userId";

    private static final String VALID_CATALOG_JSON = "{\"images\": {\"base-images\": []}, \"versions\": {}}";

    @InjectMocks
    private ImageCatalogValidator underTest;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private HttpContentSizeValidator httpContentSizeValidator;

    @Mock
    private HttpHelper httpHelper;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ConstraintViolationBuilder constraintViolationBuilder;

    @Mock
    private StatusType statusType;

    @Test
    void testValidUrlWithValidCatalogJson() {
        String url = "https://cloudbreak.example.com/catalog.json";
        when(httpContentSizeValidator.isValid(eq(url), eq(constraintValidatorContext), eq(false))).thenReturn(true);
        when(httpHelper.getContentNoRedirects(url)).thenReturn(new ImmutablePair<>(statusType, VALID_CATALOG_JSON));
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);

        assertTrue(underTest.isValid(url, constraintValidatorContext));
    }

    @Test
    void testUrlWithQueryParamsRejected() {
        String url = "https://example.com/catalog.json?token=secret";
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);

        assertFalse(underTest.isValid(url, constraintValidatorContext));

        verify(constraintValidatorContext, times(1))
                .buildConstraintViolationWithTemplate(eq(ImageCatalogUrlValidator.MSG_QUERY_PARAMS_NOT_ALLOWED));
    }

    @Test
    void testLocalAddressRejected() {
        String url = "https://127.0.0.1/catalog.json";
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);

        assertFalse(underTest.isValid(url, constraintValidatorContext));

        verify(constraintValidatorContext, times(1))
                .buildConstraintViolationWithTemplate(eq(ImageCatalogUrlValidator.MSG_LOCAL_ADDRESS_NOT_ALLOWED));
    }

    @Test
    void testRedirectResponseRejectedForNonAllowedDomain() {
        String url = "https://cloudbreak.example.com/catalog.json";
        when(httpContentSizeValidator.isValid(eq(url), eq(constraintValidatorContext), eq(false))).thenReturn(true);
        when(httpHelper.getContentNoRedirects(url)).thenReturn(new ImmutablePair<>(statusType, null));
        when(statusType.getFamily()).thenReturn(Family.REDIRECTION);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);

        assertFalse(underTest.isValid(url, constraintValidatorContext));

        verify(constraintValidatorContext, times(1))
                .buildConstraintViolationWithTemplate(eq(String.format(ImageCatalogValidator.FAILED_TO_GET_REDIRECT, url)));
    }

    @Test
    void testInvalidJsonRejected() {
        String url = "https://cloudbreak.example.com/catalog.json";
        when(httpContentSizeValidator.isValid(eq(url), eq(constraintValidatorContext), eq(false))).thenReturn(true);
        when(httpHelper.getContentNoRedirects(url)).thenReturn(new ImmutablePair<>(statusType, "not json at all"));
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);

        assertFalse(underTest.isValid(url, constraintValidatorContext));

        verify(constraintValidatorContext, times(1))
                .buildConstraintViolationWithTemplate(eq(ImageCatalogValidator.INVALID_JSON_IN_RESPONSE));
    }

    @Test
    void testInvalidJsonStructureRejected() {
        String url = "https://cloudbreak.example.com/catalog.json";
        String jsonWithoutImages = "{\"notimages\": {}}";
        when(httpContentSizeValidator.isValid(eq(url), eq(constraintValidatorContext), eq(false))).thenReturn(true);
        when(httpHelper.getContentNoRedirects(url)).thenReturn(new ImmutablePair<>(statusType, jsonWithoutImages));
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);

        assertFalse(underTest.isValid(url, constraintValidatorContext));

        verify(constraintValidatorContext, times(1))
                .buildConstraintViolationWithTemplate(eq(ImageCatalogValidator.INVALID_JSON_STRUCTURE_IN_RESPONSE));
    }

    @Test
    void testServerErrorRejected() {
        String url = "https://cloudbreak.example.com/catalog.json";
        when(httpContentSizeValidator.isValid(eq(url), eq(constraintValidatorContext), eq(false))).thenReturn(true);
        when(httpHelper.getContentNoRedirects(url)).thenReturn(new ImmutablePair<>(statusType, null));
        when(statusType.getFamily()).thenReturn(Family.SERVER_ERROR);
        when(statusType.getReasonPhrase()).thenReturn("Internal Server Error");
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);

        assertFalse(underTest.isValid(url, constraintValidatorContext));

        verify(constraintValidatorContext, times(1))
                .buildConstraintViolationWithTemplate(
                        eq(String.format(ImageCatalogValidator.FAILED_TO_GET_BY_FAMILY_TYPE, url, "Internal Server Error")));
    }

    @Test
    void testAllowedDomainWithQueryParamsAccepted() {
        String url = "https://mybucket.s3.us-east-1.amazonaws.com/catalog.json?versionId=abc";
        when(httpContentSizeValidator.isValid(eq(url), eq(constraintValidatorContext), eq(true))).thenReturn(true);
        when(httpHelper.getContent(url)).thenReturn(new ImmutablePair<>(statusType, VALID_CATALOG_JSON));
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);

        assertTrue(underTest.isValid(url, constraintValidatorContext));
    }

    @Test
    void testAllowedDomainRedirectIsFollowed() {
        String url = "https://github.com/owner/repo/raw/main/catalog.json";
        when(httpContentSizeValidator.isValid(eq(url), eq(constraintValidatorContext), eq(true))).thenReturn(true);
        when(httpHelper.getContent(url)).thenReturn(new ImmutablePair<>(statusType, VALID_CATALOG_JSON));
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);

        assertTrue(underTest.isValid(url, constraintValidatorContext));
    }

    @Test
    void testLegacyS3UrlWithoutRegionUsesRedirectFollowingHttpHelper() {
        String url = "https://cloudbreak-imagecatalog.s3.amazonaws.com/v2-dev-cb-image-catalog.json";
        when(httpContentSizeValidator.isValid(eq(url), eq(constraintValidatorContext), eq(true))).thenReturn(true);
        when(httpHelper.getContent(url)).thenReturn(new ImmutablePair<>(statusType, VALID_CATALOG_JSON));
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);

        assertTrue(underTest.isValid(url, constraintValidatorContext));

        verify(httpContentSizeValidator).isValid(eq(url), eq(constraintValidatorContext), eq(true));
        verify(httpHelper).getContent(url);
        verify(httpHelper, never()).getContentNoRedirects(anyString());
    }

    @Test
    void testPathStyleS3UrlWithoutRegionUsesRedirectFollowingHttpHelper() {
        String url = "https://s3.amazonaws.com/cloudbreak-imagecatalog/v2-dev-cb-image-catalog.json";
        when(httpContentSizeValidator.isValid(eq(url), eq(constraintValidatorContext), eq(true))).thenReturn(true);
        when(httpHelper.getContent(url)).thenReturn(new ImmutablePair<>(statusType, VALID_CATALOG_JSON));
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);

        assertTrue(underTest.isValid(url, constraintValidatorContext));

        verify(httpContentSizeValidator).isValid(eq(url), eq(constraintValidatorContext), eq(true));
        verify(httpHelper).getContent(url);
        verify(httpHelper, never()).getContentNoRedirects(anyString());
    }

    @Test
    void testNonAllowedDomainUsesNoRedirectHttpHelper() {
        String url = "https://cloudbreak.example.com/catalog.json";
        when(httpContentSizeValidator.isValid(eq(url), eq(constraintValidatorContext), eq(false))).thenReturn(true);
        when(httpHelper.getContentNoRedirects(url)).thenReturn(new ImmutablePair<>(statusType, VALID_CATALOG_JSON));
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);

        assertTrue(underTest.isValid(url, constraintValidatorContext));

        verify(httpContentSizeValidator).isValid(eq(url), eq(constraintValidatorContext), eq(false));
        verify(httpHelper).getContentNoRedirects(url);
        verify(httpHelper, never()).getContent(anyString());
    }

    @Test
    void testEntitlementDisablesStrictValidation() {
        String url = "https://mock-infrastructure:10090/mock-image-catalog?catalog-name=cb-catalog&runtime=7.2.2";
        when(entitlementService.isStrictImageCatalogUrlValidationDisabled("accountId")).thenReturn(true);
        when(httpContentSizeValidator.isValid(eq(url), eq(constraintValidatorContext), eq(true))).thenReturn(true);
        when(httpHelper.getContent(url)).thenReturn(new ImmutablePair<>(statusType, VALID_CATALOG_JSON));
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);

        boolean result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.isValid(url, constraintValidatorContext));

        assertTrue(result);
        verify(httpHelper, never()).getContentNoRedirects(anyString());
    }

    @Test
    void testEntitlementNotSetKeepsStrictValidation() {
        String url = "https://mock-infrastructure:10090/mock-image-catalog?catalog-name=cb-catalog&runtime=7.2.2";
        when(entitlementService.isStrictImageCatalogUrlValidationDisabled("accountId")).thenReturn(false);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);

        boolean result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.isValid(url, constraintValidatorContext));

        assertFalse(result);
    }
}
