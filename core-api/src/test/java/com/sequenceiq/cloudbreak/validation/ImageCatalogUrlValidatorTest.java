package com.sequenceiq.cloudbreak.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ImageCatalogUrlValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://mybucket.s3.us-east-1.amazonaws.com/catalog.json",
            "https://mybucket.s3-us-west-2.amazonaws.com/catalog.json",
            "https://s3.eu-west-1.amazonaws.com/mybucket/catalog.json",
            "https://mybucket.s3.us-east-1.amazonaws.com/catalog.json?versionId=abc"
    })
    void testAwsS3UrlsAreAllowed(String url) {
        assertTrue(ImageCatalogUrlValidator.isUrlValid(url));
        assertTrue(ImageCatalogUrlValidator.validateUrl(url).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://myaccount.blob.core.windows.net/container/catalog.json",
            "https://myaccount.blob.core.windows.net/container/catalog.json?sv=2021-06-08&sig=abc"
    })
    void testAzureBlobUrlsAreAllowed(String url) {
        assertTrue(ImageCatalogUrlValidator.isUrlValid(url));
        assertTrue(ImageCatalogUrlValidator.validateUrl(url).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://storage.googleapis.com/bucket/catalog.json",
            "https://storage.cloud.google.com/bucket/catalog.json",
            "https://mybucket.storage.googleapis.com/catalog.json"
    })
    void testGcpStorageUrlsAreAllowed(String url) {
        assertTrue(ImageCatalogUrlValidator.isUrlValid(url));
        assertTrue(ImageCatalogUrlValidator.validateUrl(url).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://github.com/owner/repo/raw/main/catalog.json",
            "https://raw.githubusercontent.com/owner/repo/main/catalog.json"
    })
    void testGithubUrlsAreAllowed(String url) {
        assertTrue(ImageCatalogUrlValidator.isUrlValid(url));
        assertTrue(ImageCatalogUrlValidator.validateUrl(url).isEmpty());
    }

    @Test
    void testQueryParamsRejectedForNonAllowedDomains() {
        String url = "https://example.com/catalog.json?token=secret";

        assertFalse(ImageCatalogUrlValidator.isUrlValid(url));
        Optional<String> result = ImageCatalogUrlValidator.validateUrl(url);
        assertTrue(result.isPresent());
        assertEquals(ImageCatalogUrlValidator.MSG_QUERY_PARAMS_NOT_ALLOWED, result.get());
    }

    @Test
    void testFragmentRejectedForNonAllowedDomains() {
        String url = "https://example.com/catalog.json#section";

        assertFalse(ImageCatalogUrlValidator.isUrlValid(url));
        Optional<String> result = ImageCatalogUrlValidator.validateUrl(url);
        assertTrue(result.isPresent());
        assertEquals(ImageCatalogUrlValidator.MSG_QUERY_PARAMS_NOT_ALLOWED, result.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://127.0.0.1/catalog.json",
            "https://10.0.0.1/catalog.json",
            "https://172.16.0.1/catalog.json",
            "https://192.168.1.1/catalog.json",
            "https://169.254.1.1/catalog.json"
    })
    void testIpAddressesRejected(String url) {
        assertFalse(ImageCatalogUrlValidator.isUrlValid(url));
        Optional<String> result = ImageCatalogUrlValidator.validateUrl(url);
        assertTrue(result.isPresent());
        assertEquals(ImageCatalogUrlValidator.MSG_LOCAL_ADDRESS_NOT_ALLOWED, result.get());
    }

    @Test
    void testLocalhostRejected() {
        String url = "https://localhost/catalog.json";

        assertFalse(ImageCatalogUrlValidator.isUrlValid(url));
        Optional<String> result = ImageCatalogUrlValidator.validateUrl(url);
        assertTrue(result.isPresent());
        assertEquals(ImageCatalogUrlValidator.MSG_LOCAL_ADDRESS_NOT_ALLOWED, result.get());
    }

    @Test
    void testIpv6Rejected() {
        String url = "https://[::1]/catalog.json";

        assertFalse(ImageCatalogUrlValidator.isUrlValid(url));
        Optional<String> result = ImageCatalogUrlValidator.validateUrl(url);
        assertTrue(result.isPresent());
        assertEquals(ImageCatalogUrlValidator.MSG_LOCAL_ADDRESS_NOT_ALLOWED, result.get());
    }

    @Test
    void testMalformedUrlRejected() {
        String url = "not-a-url";

        assertFalse(ImageCatalogUrlValidator.isUrlValid(url));
        Optional<String> result = ImageCatalogUrlValidator.validateUrl(url);
        assertTrue(result.isPresent());
        assertEquals(ImageCatalogUrlValidator.MSG_MALFORMED_URL, result.get());
    }

    @Test
    void testAllowedDomainWithNullHost() {
        assertFalse(ImageCatalogUrlValidator.isAllowedDomain(null));
        assertFalse(ImageCatalogUrlValidator.isAllowedDomain(""));
    }
}
