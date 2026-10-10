package com.sequenceiq.environment.encryptionprofile;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.tls.CipherSuiteProvider;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;

class EncryptionProfileComplianceEvaluatorTest {

    private static final String FIPS_140_3 = "FIPS_140_3";

    private static final String TLS12_GCM = "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384";

    private static final String TLS13_GCM = "TLS_AES_256_GCM_SHA384";

    private final EncryptionProfileComplianceEvaluator underTest =
            new EncryptionProfileComplianceEvaluator(new EncryptionProfileProvider(new CipherSuiteProvider()));

    @ParameterizedTest
    @ValueSource(strings = {
            "public/cdp_default_fips_140_3.json",
            "gov/cdp_default_fips_140_3.json",
            "public/cdp_default_tls13_fips_140_3.json"})
    void testBuiltInFipsProfilesAreCompliant(String resource) throws IOException {
        JsonNode profile = readProfile(resource);

        Set<String> result = underTest.evaluate(tlsVersions(profile), strings(profile.get("cipherSuites")));

        assertThat(result).containsExactly(FIPS_140_3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"public/cdp_default_v1.json", "gov/cdp_default_v1.json"})
    void testBuiltInLegacyProfileIsNotCompliant(String resource) throws IOException {
        JsonNode profile = readProfile(resource);

        Set<String> result = underTest.evaluate(tlsVersions(profile), strings(profile.get("cipherSuites")));

        assertThat(result).isEmpty();
    }

    @Test
    void testMixedTlsVersionsWithApprovedCiphersAreCompliant() {
        assertThat(underTest.evaluate(List.of(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3), List.of(TLS12_GCM, TLS13_GCM))).containsExactly(FIPS_140_3);
    }

    @Test
    void testSingleApprovedCipherPerVersionIsCompliant() {
        assertThat(underTest.evaluate(List.of(TlsVersion.TLS_1_3), List.of(TLS13_GCM))).containsExactly(FIPS_140_3);
    }

    @Test
    void testNonApprovedCipherMakesProfileNonCompliant() {
        assertThat(underTest.evaluate(List.of(TlsVersion.TLS_1_2), List.of(TLS12_GCM, "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384"))).isEmpty();
    }

    @Test
    void testTlsVersionWithoutApprovedCipherMakesProfileNonCompliant() {
        assertThat(underTest.evaluate(List.of(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3), List.of(TLS12_GCM))).isEmpty();
    }

    @Test
    void testApprovedCipherOfOtherVersionDoesNotServeVersion() {
        assertThat(underTest.evaluate(List.of(TlsVersion.TLS_1_2), List.of(TLS13_GCM))).isEmpty();
    }

    @Test
    void testEmptyOrNullInputIsNotCompliant() {
        assertThat(underTest.evaluate(List.of(), List.of(TLS12_GCM))).isEmpty();
        assertThat(underTest.evaluate(List.of(TlsVersion.TLS_1_2), List.of())).isEmpty();
        assertThat(underTest.evaluate(null, null)).isEmpty();
    }

    private JsonNode readProfile(String resource) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/defaults/encryptionprofile/" + resource)) {
            return new ObjectMapper().readTree(stream);
        }
    }

    private List<TlsVersion> tlsVersions(JsonNode profile) {
        return strings(profile.get("tlsVersions")).stream().map(TlsVersion::fromString).toList();
    }

    private List<String> strings(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false).map(JsonNode::asText).toList();
    }
}
