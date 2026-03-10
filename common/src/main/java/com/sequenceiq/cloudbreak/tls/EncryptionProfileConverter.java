package com.sequenceiq.cloudbreak.tls;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class EncryptionProfileConverter {

    private static final String COMMA_SEPARATOR = ",";

    private EncryptionProfileConverter() {
    }

    public static String getTlsVersionsSeparatedByComma(Set<String> userTlsVersions) {
        return EncryptionProfileConverter.mergeTlsVersions(userTlsVersions, COMMA_SEPARATOR);
    }

    public static String getTlsVersionsSeparatedBySpace(Set<String> userTlsVersions) {
        return EncryptionProfileConverter.mergeTlsVersions(userTlsVersions, StringUtils.SPACE);
    }

    private static String mergeTlsVersions(Set<String> userTlsVersions, String separator) {
        return userTlsVersions
                .stream()
                .sorted()
                .collect(Collectors.joining(separator));
    }

    public static List<CipherSuite> toCipherSuites(List<String> input) {
        try {
            return input
                    .stream()
                    .map(CipherSuite::valueOf)
                    .toList();
        } catch (Exception e) {
            throw new CloudbreakServiceException("Failed to convert to CipherSuite.", e);
        }
    }

    public static List<String> toListString(List<CipherSuite> input) {
            return input
                    .stream()
                    .map(CipherSuite::getIanaName)
                    .toList();
    }

    public static String toString(List<CipherSuite> input, boolean useIanaName, String separator) {
        return input.stream()
                .map(useIanaName ? CipherSuite::getIanaName : CipherSuite::getOpenSslName)
                .distinct()
                .collect(Collectors.joining(separator));
    }

    public static String fromListToString(List<String> input, boolean useIanaName, String separator) {
        List<CipherSuite> cipherSuites = toCipherSuites(input);
        return toString(cipherSuites, useIanaName, separator);
    }

    public static String mergeCipherSuites(String tls13Ciphers, String tls12Ciphers, String separator) {
        return Stream.of(tls13Ciphers, tls12Ciphers)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining(separator));
    }
}
