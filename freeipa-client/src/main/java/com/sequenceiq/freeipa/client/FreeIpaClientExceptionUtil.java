package com.sequenceiq.freeipa.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;

import com.googlecode.jsonrpc4j.JsonRpcClientException;

public class FreeIpaClientExceptionUtil {

    // See https://github.com/freeipa/freeipa/blob/master/ipalib/errors.py
    public enum ErrorCodes {
        PUBLIC_ERROR(900),
        VERSION_ERROR(901),
        UNKNOWN_ERROR(902),
        INTERNAL_ERROR(903),
        SERVER_INTERNAL_ERROR(904),
        COMMAND_ERROR(905),
        SERVER_COMMAND_ERROR(906),
        NETWORK_ERROR(907),
        SERVER_NETWORK_ERROR(908),
        JSON_ERROR(909),
        XML_RPC_MARSHALL_ERROR(910),
        REFERER_ERROR(911),
        ENVIRONMENT_ERROR(912),
        SYSTEM_ENCODING_ERROR(913),
        AUTHENTICATION_ERROR(1000),
        KERBEROS_ERROR(1100),
        CCACHE_ERROR(1101),
        SERVICE_ERROR(1102),
        NO_CACHE_ERROR(1103),
        TICKET_EXPIRED(1104),
        BAD_CCACHE_PERMS(1105),
        BAD_CCACHE_FORMAT(1106),
        CANNOT_RESOLVE_KDC(1107),
        SESSION_ERROR(1200),
        INVALID_SESSION_PASSWORD(1201),
        PASSWORD_EXPIRED(1202),
        KRB_PRINCIPAL_EXPIRED(1203),
        USER_LOCKED(1204),
        AUTHORIZATION_ERROR(2000),
        ACI_ERROR(2100),
        INVOCATION_ERROR(3000),
        ENCODING_ERROR(3001),
        BINARY_ENCODING_ERROR(3002),
        ZERO_ARGUMENT_ERROR(3003),
        MAX_ARGUMENT_ERROR(3004),
        OPTION_ERROR(3005),
        OVERLAP_ERROR(3006),
        REQUIREMENT_ERROR(3007),
        CONVERSION_ERROR(3008),
        VALIDATION_ERROR(3009),
        NO_SUCH_NAMESPACE_ERROR(3010),
        PASSWORD_MISMATCH(3011),
        NOT_IMPLEMENTED_ERROR(3012),
        NOT_CONFIGURED_ERROR(3013),
        PROMPT_FAILED(3014),
        DEPRECATION_ERROR(3015),
        NOT_A_FOREST_ROOT_ERROR(3016),
        EXECUTION_ERROR(4000),
        NOT_FOUND(4001),
        DUPLICATE_ENTRY(4002),
        HOST_SERVICE(4003),
        MALFORMED_SERVICE_PRINCIPAL(4004),
        REALM_MISMATCH(4005),
        REQUIRES_ROOT(4006),
        ALREADY_POSIX_GROUP(4007),
        MALFORMED_USER_PRINCIPAL(4008),
        ALREADY_ACTIVE(4009),
        ALREADY_INACTIVE(4010),
        HAS_NS_ACCOUNT_LOCK(4011),
        NOT_GROUP_MEMBER(4012),
        RECURSIVE_GROUP(4013),
        ALREADY_GROUP_MEMBER(4014),
        BASE64_DECODE_ERROR(4015),
        REMOTE_RETRIEVE_ERROR(4016),
        SAME_GROUP_ERROR(4017),
        DEFAULT_GROUP_ERROR(4018),
        MANAGED_GROUP_ERROR(4020),
        MANAGED_POLICY_ERROR(4021),
        FILE_ERROR(4022),
        NO_CERTIFICATE_ERROR(4023),
        MANAGED_GROUP_EXISTS_ERROR(4024),
        REVERSE_MEMBER_ERROR(4025),
        ATTRIBUTE_VALUE_NOT_FOUND(4026),
        SINGLE_MATCH_EXPECTED(4027),
        ALREADY_EXTERNAL_GROUP(4028),
        EXTERNAL_GROUP_VIOLATION(4029),
        POSIX_GROUP_VIOLATION(4030),
        EMPTY_RESULT(4031),
        INVALID_DOMAIN_LEVEL_ERROR(4032),
        SERVER_REMOVAL_ERROR(4033),
        OPERATION_NOT_SUPPORTED_FOR_PRINCIPAL_TYPE(4034),
        HTTP_REQUEST_ERROR(4035),
        REDUNDANT_MAPPING_RULE(4036),
        CSR_TEMPLATE_ERROR(4037),
        BUILTIN_ERROR(4100),
        HELP_ERROR(4101),
        LDAP_ERROR(4200),
        MIDAIR_COLLISION(4201),
        EMPTY_MODLIST(4202),
        DATABASE_ERROR(4203),
        LIMIT_EXCEEDED(4204),
        OBJECTCLASS_VIOLATION(4205),
        NOT_ALLOWED_ON_RDN(4206),
        ONLY_ONE_VALUE_ALLOWED(4207),
        INVALID_SYNTAX(4208),
        BAD_SEARCH_FILTER(4209),
        NOT_ALLOWED_ON_NON_LEAF(4210),
        DATABASE_TIMEOUT(4211),
        TASK_TIMEOUT(4213),
        TIME_LIMIT_EXCEEDED(4214),
        SIZE_LIMIT_EXCEEDED(4215),
        ADMIN_LIMIT_EXCEEDED(4216),
        CERTIFICATE_ERROR(4300),
        CERTIFICATE_OPERATION_ERROR(4301),
        CERTIFICATE_FORMAT_ERROR(4302),
        MUTUALLY_EXCLUSIVE_ERROR(4303),
        NON_FATAL_ERROR(4304),
        ALREADY_REGISTERED_ERROR(4305),
        NOT_REGISTERED_ERROR(4306),
        DEPENDENT_ENTRY(4307),
        LAST_MEMBER_ERROR(4308),
        PROTECTED_ENTRY_ERROR(4309),
        CERTIFICATE_INVALID_ERROR(4310),
        SCHEMA_UP_TO_DATE(4311),
        DNS_ERROR(4400),
        DNS_NOT_A_RECORED_ERROR(4019),
        DNS_DATA_MISMATCH(4212),
        DNS_RESOLVER_ERROR(4401),
        TRUST_ERROR(4500),
        TRUST_TOPOLOGY_CONFLICT_ERROR(4501),
        GENERIC_ERROR(5000);

        private final int value;

        ErrorCodes(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private static final Map<Integer, ErrorCodes> ERROR_CODES_LOOKUP = new HashMap<>();

    private static final Set<ErrorCodes> RETRYABLE_HTTP_CODES = Set.of(
            ErrorCodes.NETWORK_ERROR,
            ErrorCodes.SERVER_NETWORK_ERROR,
            ErrorCodes.REFERER_ERROR,
            ErrorCodes.AUTHENTICATION_ERROR,
            ErrorCodes.KERBEROS_ERROR,
            ErrorCodes.CCACHE_ERROR,
            ErrorCodes.SERVICE_ERROR,
            ErrorCodes.NO_CACHE_ERROR,
            ErrorCodes.TICKET_EXPIRED,
            ErrorCodes.BAD_CCACHE_PERMS,
            ErrorCodes.BAD_CCACHE_FORMAT,
            ErrorCodes.CANNOT_RESOLVE_KDC,
            ErrorCodes.SESSION_ERROR,
            ErrorCodes.INVALID_SESSION_PASSWORD,
            ErrorCodes.AUTHORIZATION_ERROR,
            ErrorCodes.HTTP_REQUEST_ERROR,
            ErrorCodes.MIDAIR_COLLISION,
            ErrorCodes.LIMIT_EXCEEDED,
            ErrorCodes.DATABASE_TIMEOUT,
            ErrorCodes.TASK_TIMEOUT,
            ErrorCodes.TIME_LIMIT_EXCEEDED,
            ErrorCodes.SIZE_LIMIT_EXCEEDED,
            ErrorCodes.ADMIN_LIMIT_EXCEEDED,
            ErrorCodes.NON_FATAL_ERROR,
            ErrorCodes.GENERIC_ERROR
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
            .map(s -> s.value())
            .collect(Collectors.toSet());

    static {
        for (ErrorCodes errorCode : ErrorCodes.values()) {
            ERROR_CODES_LOOKUP.put(errorCode.value, errorCode);
        }
    }

    private FreeIpaClientExceptionUtil() {
    }

    public static boolean isNotFoundException(FreeIpaClientException e) {
        return isExceptionWithErrorCode(e, Set.of(ErrorCodes.NOT_FOUND));
    }

    public static boolean isDuplicateEntryException(FreeIpaClientException e) {
        return isExceptionWithErrorCode(e, Set.of(ErrorCodes.DUPLICATE_ENTRY));
    }

    public static boolean isExceptionWithErrorCode(FreeIpaClientException e, Set<ErrorCodes> errorCodes) {
        return Optional.ofNullable(getAncestorCauseBeforeFreeIpaClientExceptions(e))
                .filter(JsonRpcClientException.class::isInstance)
                .map(JsonRpcClientException.class::cast)
                .map(JsonRpcClientException::getCode)
                .flatMap(c -> Optional.ofNullable(ERROR_CODES_LOOKUP.get(c)))
                .filter(c -> errorCodes.contains(c))
                .isPresent();
    }

    public static FreeIpaClientException convertToRetryableIfNeeded(FreeIpaClientException e) {
        if (isRetryable(e)) {
            return new RetryableFreeIpaClientException(e.getLocalizedMessage(), e, e.getStatusCode());
        } else {
            return e;
        }
    }

    private static boolean isRetryable(FreeIpaClientException e) {
        return isExceptionWithErrorCode(e, RETRYABLE_HTTP_CODES) ||
                (e.getStatusCode().isPresent() &&
                retryableHttpCodes.contains(e.getStatusCode().getAsInt()));
    }

    public static Throwable getAncestorCauseBeforeFreeIpaClientExceptions(FreeIpaClientException e) {
        FreeIpaClientException currentException = e;
        while (currentException.getCause() instanceof FreeIpaClientException) {
            currentException = (FreeIpaClientException) currentException.getCause();
        }
        return currentException.getCause();
    }
}
