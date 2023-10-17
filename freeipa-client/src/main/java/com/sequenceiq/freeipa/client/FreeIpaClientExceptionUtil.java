package com.sequenceiq.freeipa.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.google.common.annotations.VisibleForTesting;
import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyError;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyException;

public class FreeIpaClientExceptionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaClientExceptionUtil.class);

    private static final Map<Integer, FreeIpaErrorCodes> ERROR_CODES_LOOKUP = new HashMap<>();

    private static final Pattern RESPONSE_CODE_PATTERN = Pattern.compile("^Server returned HTTP response code: (\\d+)");

    private static final Set<FreeIpaErrorCodes> RETRYABLE_ERROR_CODES = Set.of(
            FreeIpaErrorCodes.NETWORK_ERROR,
            FreeIpaErrorCodes.SERVER_NETWORK_ERROR,
            FreeIpaErrorCodes.REFERER_ERROR,
            FreeIpaErrorCodes.AUTHENTICATION_ERROR,
            FreeIpaErrorCodes.KERBEROS_ERROR,
            FreeIpaErrorCodes.CCACHE_ERROR,
            FreeIpaErrorCodes.SERVICE_ERROR,
            FreeIpaErrorCodes.NO_CACHE_ERROR,
            FreeIpaErrorCodes.TICKET_EXPIRED,
            FreeIpaErrorCodes.BAD_CCACHE_PERMS,
            FreeIpaErrorCodes.BAD_CCACHE_FORMAT,
            FreeIpaErrorCodes.CANNOT_RESOLVE_KDC,
            FreeIpaErrorCodes.SESSION_ERROR,
            FreeIpaErrorCodes.INVALID_SESSION_PASSWORD,
            FreeIpaErrorCodes.AUTHORIZATION_ERROR,
            FreeIpaErrorCodes.HTTP_REQUEST_ERROR,
            FreeIpaErrorCodes.MIDAIR_COLLISION,
            FreeIpaErrorCodes.LIMIT_EXCEEDED,
            FreeIpaErrorCodes.DATABASE_TIMEOUT,
            FreeIpaErrorCodes.TASK_TIMEOUT,
            FreeIpaErrorCodes.TIME_LIMIT_EXCEEDED,
            FreeIpaErrorCodes.SIZE_LIMIT_EXCEEDED,
            FreeIpaErrorCodes.ADMIN_LIMIT_EXCEEDED,
            FreeIpaErrorCodes.NON_FATAL_ERROR,
            FreeIpaErrorCodes.GENERIC_ERROR,
            FreeIpaErrorCodes.ACI_ERROR
    );

    private static final Set<FreeIpaErrorCodes> CLIENT_UNUSABLE_ERROR_CODES = Set.of(
            FreeIpaErrorCodes.AUTHENTICATION_ERROR,
            FreeIpaErrorCodes.SESSION_ERROR
    );

    private static Set<Integer> retryableHttpCodes = Set.of(
            HttpStatus.UNAUTHORIZED,
            HttpStatus.PROXY_AUTHENTICATION_REQUIRED,
            HttpStatus.REQUEST_TIMEOUT,
            HttpStatus.TOO_MANY_REQUESTS,
            HttpStatus.INTERNAL_SERVER_ERROR,
            HttpStatus.BAD_GATEWAY,
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.BANDWIDTH_LIMIT_EXCEEDED,
            HttpStatus.NETWORK_AUTHENTICATION_REQUIRED
    )
            .stream()
            .map(HttpStatus::value)
            .collect(Collectors.toSet());

    private static final Set<String> CLUSTERPROXY_RETRYABLE_CODES = Set.of("cluster-proxy.proxy.endpoint-or-knox-required");

    private static Set<Integer> clientUnusableHttpCodes = Set.of(HttpStatus.UNAUTHORIZED)
            .stream().map(HttpStatus::value).collect(Collectors.toSet());

    static {
        for (FreeIpaErrorCodes errorCode : FreeIpaErrorCodes.values()) {
            ERROR_CODES_LOOKUP.put(errorCode.getValue(), errorCode);
        }
    }

    private FreeIpaClientExceptionUtil() {
    }

    public static boolean isNotFoundException(FreeIpaClientException e) {
        return isExceptionWithErrorCode(e, Set.of(FreeIpaErrorCodes.NOT_FOUND));
    }

    public static boolean isDuplicateEntryException(FreeIpaClientException e) {
        return isExceptionWithErrorCode(e, Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY));
    }

    public static boolean isEmptyModlistException(FreeIpaClientException e) {
        return isExceptionWithErrorCode(e, Set.of(FreeIpaErrorCodes.EMPTY_MODLIST));
    }

    public static boolean isExceptionWithErrorCode(FreeIpaClientException e, Set<FreeIpaErrorCodes> errorCodes) {
        return Stream.of(getAncestorCauseBeforeFreeIpaClientExceptions(e))
                .filter(JsonRpcClientException.class::isInstance)
                .map(JsonRpcClientException.class::cast)
                .map(JsonRpcClientException::getCode)
                .peek(code -> LOGGER.debug("Error code found: [{}]", code))
                .map(FreeIpaClientExceptionUtil::findFreeIpaErrorCodeByCode)
                .peek(code -> LOGGER.debug("Error code: [{}]", code))
                .anyMatch(c -> isFreeIpaErrorCodeInSet(errorCodes, c));
    }

    @VisibleForTesting
    static boolean isExceptionWithIOExceptionCause(FreeIpaClientException e) {
        return ExceptionUtils.getThrowableList(e)
                .stream()
                .anyMatch(IOException.class::isInstance);
    }

    private static boolean isFreeIpaErrorCodeInSet(Set<FreeIpaErrorCodes> errorCodes, FreeIpaErrorCodes c) {
        return errorCodes.contains(c);
    }

    private static FreeIpaErrorCodes findFreeIpaErrorCodeByCode(Integer code) {
        return ERROR_CODES_LOOKUP.get(code);
    }

    public static FreeIpaClientException convertToRetryableIfNeeded(FreeIpaClientException e) {
        if (isRetryable(e)) {
            return new RetryableFreeIpaClientException(e.getLocalizedMessage(), e, e.getStatusCode());
        } else {
            return e;
        }
    }

    public static void ignoreNotFoundException(FreeIpaClientRunnable runnable, String message, Object... messageParams) throws FreeIpaClientException {
        try {
            runnable.run();
        } catch (FreeIpaClientException e) {
            if (isNotFoundException(e)) {
                Optional.ofNullable(message).ifPresentOrElse(
                        msg -> LOGGER.debug(msg, messageParams),
                        () -> LOGGER.debug("Not found in FreeIPA ignored. Exception message: {}", e.getMessage()));
            } else {
                throw e;
            }
        }
    }

    public static <T> Optional<T> ignoreNotFoundExceptionWithValue(FreeIpaClientCallable<T> callable, String message, Object... messageParams)
            throws FreeIpaClientException {
        try {
            return Optional.ofNullable(callable.run());
        } catch (FreeIpaClientException e) {
            if (isNotFoundException(e)) {
                Optional.ofNullable(message).ifPresentOrElse(
                        msg -> LOGGER.debug(msg, messageParams),
                        () -> LOGGER.debug("Not found in FreeIPA ignored. Exception message: {}", e.getMessage()));
                return Optional.empty();
            } else {
                throw e;
            }
        }
    }

    public static <T> Optional<T> ignoreEmptyModExceptionWithValue(FreeIpaClientCallable<T> callable, String message, Object... messageParams)
            throws FreeIpaClientException {
        try {
            return Optional.ofNullable(callable.run());
        } catch (FreeIpaClientException e) {
            if (isEmptyModlistException(e)) {
                Optional.ofNullable(message).ifPresentOrElse(
                        msg -> LOGGER.debug(msg, messageParams),
                        () -> LOGGER.debug("No modification was needed in FreeIPA is ignored. Exception message: {}", e.getMessage()));
                return Optional.empty();
            } else {
                throw e;
            }
        }
    }

    public static void ignoreEmptyModException(FreeIpaClientRunnable runnable, String message, Object... messageParams)
            throws FreeIpaClientException {
        try {
            runnable.run();
        } catch (FreeIpaClientException e) {
            if (isEmptyModlistException(e)) {
                Optional.ofNullable(message).ifPresentOrElse(
                        msg -> LOGGER.debug(msg, messageParams),
                        () -> LOGGER.debug("No modification was needed in FreeIPA is ignored. Exception message: {}", e.getMessage()));
            } else {
                throw e;
            }
        }
    }

    public static <T> Optional<T> ignoreDuplicateExceptionWithValue(FreeIpaClientCallable<T> callable, String message, Object... messageParams)
            throws FreeIpaClientException {
        try {
            return Optional.ofNullable(callable.run());
        } catch (FreeIpaClientException e) {
            if (isDuplicateEntryException(e)) {
                Optional.ofNullable(message).ifPresentOrElse(
                        msg -> LOGGER.debug(msg, messageParams),
                        () -> LOGGER.debug("Already present in FreeIPA, exception is ignored. Exception message: {}", e.getMessage()));
                return Optional.empty();
            } else {
                throw e;
            }
        }
    }

    private static boolean isRetryable(FreeIpaClientException e) {
        return isExceptionWithErrorCode(e, RETRYABLE_ERROR_CODES) || isExceptionWithHttpCode(retryableHttpCodes, e) || isExceptionWithIOExceptionCause(e);
    }

    private static boolean isExceptionWithHttpCode(Set<Integer> codes, FreeIpaClientException e) {
        return e.getStatusCode().isPresent() && codes.contains(e.getStatusCode().getAsInt());
    }

    public static boolean isClientUnusable(FreeIpaClientException e) {
        return isExceptionWithErrorCode(e, CLIENT_UNUSABLE_ERROR_CODES) || isExceptionWithHttpCode(clientUnusableHttpCodes, e);
    }

    public static Throwable getAncestorCauseBeforeFreeIpaClientExceptions(FreeIpaClientException e) {
        FreeIpaClientException currentException = e;
        while (currentException.getCause() instanceof FreeIpaClientException) {
            currentException = (FreeIpaClientException) currentException.getCause();
        }
        return currentException.getCause();
    }

    public static OptionalInt extractResponseCode(Exception e) {
        try {
            Matcher matcher = RESPONSE_CODE_PATTERN.matcher(e.getMessage());
            if (matcher.find()) {
                return OptionalInt.of(Integer.parseInt(matcher.group(1)));
            } else if (null != e.getCause() && RESPONSE_CODE_PATTERN.matcher(e.getCause().getMessage()).find()) {
                matcher = RESPONSE_CODE_PATTERN.matcher(e.getCause().getMessage());
                matcher.find();
                return OptionalInt.of(Integer.parseInt(matcher.group(1)));
            } else {
                return OptionalInt.empty();
            }
        } catch (Exception ex) {
            LOGGER.warn("Couldn't extract response code from message", ex);
            return OptionalInt.empty();
        }
    }

    public static OptionalInt extractResponseCode(ClusterProxyException e) {
        try {
            return e.getClusterProxyError()
                    .map(ClusterProxyError::getStatus)
                    .stream()
                    .mapToInt(Integer::parseInt)
                    .findAny();
        } catch (Exception ex) {
            LOGGER.warn("Couldn't extract response code from message", ex);
            return OptionalInt.empty();
        }
    }

    public static boolean isClusterProxyErrorRetryable(ClusterProxyException e) {
        LOGGER.error("Cluster proxy exception received", e);
        return e.getClusterProxyError().map(ClusterProxyError::getCode).map(CLUSTERPROXY_RETRYABLE_CODES::contains).orElse(Boolean.FALSE);
    }
}
