package com.sequenceiq.cloudbreak.validation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageCatalogUrlValidator {

    public static final String MSG_QUERY_PARAMS_NOT_ALLOWED =
            "The specified URL contains query parameters or fragments which are not allowed for image catalogs!";

    public static final String MSG_LOCAL_ADDRESS_NOT_ALLOWED =
            "The specified URL points to a local or private network address which is not allowed for image catalogs!";

    public static final String MSG_MALFORMED_URL = "The specified URL is malformed or has invalid syntax!";

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogUrlValidator.class);

    private static final Pattern AWS_S3_PATTERN = Pattern.compile(
            "^([^.]+\\.)?s3[.-][^.]+\\.amazonaws\\.com$", Pattern.CASE_INSENSITIVE
    );

    private static final Pattern AZURE_BLOB_PATTERN = Pattern.compile(
            "^[^.]+\\.blob\\.core\\.windows\\.net$", Pattern.CASE_INSENSITIVE
    );

    private static final Pattern GCP_STORAGE_PATTERN = Pattern.compile(
            "^(storage\\.googleapis\\.com|storage\\.cloud\\.google\\.com|[^.]+\\.storage\\.googleapis\\.com)$", Pattern.CASE_INSENSITIVE
    );

    private static final Pattern GITHUB_PATTERN = Pattern.compile(
            "^(github\\.com|raw\\.githubusercontent\\.com)$", Pattern.CASE_INSENSITIVE
    );

    private ImageCatalogUrlValidator() {

    }

    public static Optional<String> validateUrl(String urlString) {
        try {
            URI uri = new URI(urlString);
            String host = uri.getHost();

            if (host == null || host.isEmpty()) {
                return Optional.of(MSG_MALFORMED_URL);
            }

            if (isAllowedDomain(host)) {
                return Optional.empty();
            }

            if (uri.getQuery() != null || uri.getFragment() != null) {
                return Optional.of(MSG_QUERY_PARAMS_NOT_ALLOWED);
            }

            if (isIpAddressOrInternal(host)) {
                return Optional.of(MSG_LOCAL_ADDRESS_NOT_ALLOWED);
            }

            return Optional.empty();
        } catch (URISyntaxException | IllegalArgumentException e) {
            LOGGER.error("Input URL is malformed or violating proper URI syntax, reason: ", e);
            return Optional.of(MSG_MALFORMED_URL);
        }
    }

    public static boolean isUrlValid(String urlString) {
        return validateUrl(urlString).isEmpty();
    }

    private static boolean isIpAddressOrInternal(String host) {
        if (host == null || host.isEmpty()) {
            return true;
        }
        if ("localhost".equalsIgnoreCase(host)) {
            return true;
        }
        if (host.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
            return true;
        }
        if (host.contains(":")) {
            return true;
        }
        return false;
    }

    public static boolean isAllowedDomain(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }

        return AWS_S3_PATTERN.matcher(host).matches() ||
                AZURE_BLOB_PATTERN.matcher(host).matches() ||
                GCP_STORAGE_PATTERN.matcher(host).matches() ||
                GITHUB_PATTERN.matcher(host).matches();
    }
}
