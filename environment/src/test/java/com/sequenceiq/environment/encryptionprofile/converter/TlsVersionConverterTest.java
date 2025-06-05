package com.sequenceiq.environment.encryptionprofile.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.api.encryptionprofile.TlsVersion;

@ExtendWith(MockitoExtension.class)
class TlsVersionConverterTest {

    private TlsVersionConverter converter;

    @BeforeEach
    void setUp() {
        converter = new TlsVersionConverter();
    }

    @Test
    void testConvertToDatabaseColumnWithMultipleValues() {
        Set<TlsVersion> input = new HashSet<>(Arrays.asList(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3));
        String result = converter.convertToDatabaseColumn(input);

        assertThat(result).contains("TLSv1.2", "TLSv1.3");
        assertThat(result.chars().filter(ch -> ch == ',').count()).isEqualTo(1);
    }

    @Test
    void testConvertToDatabaseColumnWithEmptySet() {
        Set<TlsVersion> input = Collections.emptySet();
        String result = converter.convertToDatabaseColumn(input);

        assertThat(result).isEmpty();
    }

    @Test
    void testConvertToDatabaseColumnWithNull() {
        String result = converter.convertToDatabaseColumn(null);

        assertThat(result).isEmpty();
    }

    @Test
    void testConvertToEntityAttributeWithValidString() {
        String dbData = "TLSv1.2,TLSv1.3";
        Set<TlsVersion> result = converter.convertToEntityAttribute(dbData);

        assertThat(result)
                .containsExactlyInAnyOrder(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
                .hasSize(2);
    }

    @Test
    void testConvertToEntityAttributeWithEmptyString() {
        String dbData = "";
        Set<TlsVersion> result = converter.convertToEntityAttribute(dbData);

        assertThat(result).isEmpty();
    }

    @Test
    void testConvertToEntityAttributeWithNull() {
        Set<TlsVersion> result = converter.convertToEntityAttribute(null);

        assertThat(result).isEmpty();
    }
}
