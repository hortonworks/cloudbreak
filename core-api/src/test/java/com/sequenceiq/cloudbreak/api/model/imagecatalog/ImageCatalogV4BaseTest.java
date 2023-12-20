package com.sequenceiq.cloudbreak.api.model.imagecatalog;

import static com.sequenceiq.cloudbreak.validation.ImageCatalogValidator.FAILED_TO_GET_WITH_EXCEPTION;
import static com.sequenceiq.cloudbreak.validation.ImageCatalogValidator.INVALID_JSON_IN_RESPONSE;
import static com.sequenceiq.cloudbreak.validation.ImageCatalogValidator.INVALID_JSON_STRUCTURE_IN_RESPONSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Set;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintViolation;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.base.ImageCatalogV4Base;
import com.sequenceiq.cloudbreak.api.helper.HttpHelper;
import com.sequenceiq.cloudbreak.validation.HttpContentSizeValidator;

@ExtendWith(MockitoExtension.class)
public class ImageCatalogV4BaseTest extends ValidatorTestHelper {

    public static final String FAILED_TO_GET_BY_FAMILY_TYPE = "Failed to get response by the specified URL '%s' due to: '%s'!";

    private static final String INVALID_MESSAGE = "A valid image catalog must be available on the given URL";

    @Mock
    private StatusType statusType;

    @MockBean
    private HttpHelper httpHelper;

    @MockBean
    private HttpContentSizeValidator httpContentSizeValidator;

    @BeforeEach
    public void setUp() {
        when(httpContentSizeValidator.isValid(anyString(), any(ConstraintValidatorContext.class))).thenReturn(true);
    }

    @Test
    public void testContentNotAvailable() {
        String url = "http://protocol.com";
        String reasonPhrase = "Invalid reason phrase";
        when(statusType.getFamily()).thenReturn(Family.OTHER);
        when(statusType.getReasonPhrase()).thenReturn(reasonPhrase);
        when(httpHelper.getContent(anyString())).thenReturn(new ImmutablePair<>(statusType, ""));

        ImageCatalogV4Base i = new ImageCatalogV4Base();
        i.setName("testname");
        i.setUrl(url);

        Set<ConstraintViolation<ImageCatalogV4Base>> violations = getValidator().validate(i);

        assertEquals(2L, violations.size());
        String failedToGetMessage = String.format(FAILED_TO_GET_BY_FAMILY_TYPE, url, reasonPhrase);
        assertTrue(violations.stream().allMatch(cv -> cv.getMessage().equals(INVALID_MESSAGE) || cv.getMessage().equals(failedToGetMessage)));
    }

    @Test
    public void testContentStructureNotValid() {
        when(httpHelper.getContent(anyString())).thenReturn(new ImmutablePair<>(statusType, "{}"));
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);

        ImageCatalogV4Base i = new ImageCatalogV4Base();
        i.setName("testname");
        i.setUrl("http://protocol.com");

        Set<ConstraintViolation<ImageCatalogV4Base>> violations = getValidator().validate(i);

        assertEquals(2L, violations.size());
        assertTrue(violations.stream().allMatch(cv -> cv.getMessage().equals(INVALID_MESSAGE) ||
                cv.getMessage().equals(INVALID_JSON_STRUCTURE_IN_RESPONSE)));
    }

    @Test
    public void testContentNotAValidJSON() {
        when(httpHelper.getContent(anyString())).thenReturn(new ImmutablePair<>(statusType, "{[]}"));
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);

        ImageCatalogV4Base i = new ImageCatalogV4Base();
        i.setName("testname");
        i.setUrl("http://protocol.com");

        Set<ConstraintViolation<ImageCatalogV4Base>> violations = getValidator().validate(i);

        assertEquals(2L, violations.size());
        assertTrue(violations.stream().allMatch(cv -> cv.getMessage().equals(INVALID_MESSAGE) || cv.getMessage().equals(INVALID_JSON_IN_RESPONSE)));
    }

    @Test
    public void testWhenWebTargetFailsWithException() {
        when(httpHelper.getContent(anyString())).thenThrow(ProcessingException.class);

        ImageCatalogV4Base i = new ImageCatalogV4Base();
        i.setName("testname");
        i.setUrl("http://protocol.com");

        Set<ConstraintViolation<ImageCatalogV4Base>> violations = getValidator().validate(i);

        assertEquals(2L, violations.size());
        String failsWithExceptionMessage = String.format(FAILED_TO_GET_WITH_EXCEPTION, i.getUrl());
        assertTrue(violations.stream().allMatch(cv -> cv.getMessage().equals(INVALID_MESSAGE) || cv.getMessage().equals(failsWithExceptionMessage)));
    }

    @Test
    public void testWhenContentIsTooBig() {
        when(httpContentSizeValidator.isValid(anyString(), any(ConstraintValidatorContext.class))).thenReturn(false);

        ImageCatalogV4Base i = new ImageCatalogV4Base();
        i.setName("testname");
        i.setUrl("http://protocol.com");

        Set<ConstraintViolation<ImageCatalogV4Base>> violations = getValidator().validate(i);

        assertEquals(1L, violations.size());
        assertTrue(violations.stream().allMatch(cv -> cv.getMessage().equals(INVALID_MESSAGE)));
    }
}
