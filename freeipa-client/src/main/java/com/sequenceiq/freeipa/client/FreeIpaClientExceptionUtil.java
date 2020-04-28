package com.sequenceiq.freeipa.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.googlecode.jsonrpc4j.JsonRpcClientException;

public class FreeIpaClientExceptionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaClientExceptionUtil.class);

    private static final Map<Integer, FreeIpaErrorCodes> ERROR_CODES_LOOKUP = new HashMap<>();

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
            FreeIpaErrorCodes.GENERIC_ERROR
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

    private static boolean isRetryable(FreeIpaClientException e) {
        return isExceptionWithErrorCode(e, RETRYABLE_ERROR_CODES) || isExceptionWithHttpCode(retryableHttpCodes, e);
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
}
