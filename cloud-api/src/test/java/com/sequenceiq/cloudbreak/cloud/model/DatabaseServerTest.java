package com.sequenceiq.cloudbreak.cloud.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class DatabaseServerTest {

    @Test
    void builderTest() {
        assertThat(DatabaseServer.builder()).isNotNull();
    }

    @Test
    void fallbackInstanceTypesDefaultsToEmptyList() {
        DatabaseServer databaseServer = DatabaseServer.builder().build();

        assertThat(databaseServer.getFallbackInstanceTypes()).isEmpty();
    }

    @Test
    void fallbackInstanceTypesNullResetsToEmptyList() {
        DatabaseServer databaseServer = DatabaseServer.builder().withFallbackInstanceTypes(null).build();

        assertThat(databaseServer.getFallbackInstanceTypes()).isEmpty();
    }

    @Test
    void copyBuilderPreservesFallbackInstanceTypes() {
        DatabaseServer original = DatabaseServer.builder()
                .withFlavor("db.m5.large")
                .withFallbackInstanceTypes(List.of("db.m6i.large", "db.m7i.large"))
                .build();

        DatabaseServer copy = DatabaseServer.builder(original).withFlavor("db.m6i.large").build();

        assertThat(copy.getFlavor()).isEqualTo("db.m6i.large");
        assertThat(copy.getFallbackInstanceTypes()).containsExactly("db.m6i.large", "db.m7i.large");
    }

    @Test
    void isUseSslEnforcementTestWhenDefault() {
        DatabaseServer databaseServer = DatabaseServer.builder().build();

        assertThat(databaseServer).isNotNull();
        assertThat(databaseServer.isUseSslEnforcement()).isFalse();
    }

    @Test
    void isUseSslEnforcementTestWhenFalse() {
        DatabaseServer databaseServer = DatabaseServer.builder().withUseSslEnforcement(false).build();

        assertThat(databaseServer).isNotNull();
        assertThat(databaseServer.isUseSslEnforcement()).isFalse();
    }

    @Test
    void isUseSslEnforcementTestWhenTrue() {
        DatabaseServer databaseServer = DatabaseServer.builder().withUseSslEnforcement(true).build();

        assertThat(databaseServer).isNotNull();
        assertThat(databaseServer.isUseSslEnforcement()).isTrue();
    }

    @Test
    void toStringTestWhenSslEnforcement() {
        DatabaseServer databaseServer = DatabaseServer.builder().withUseSslEnforcement(true).build();

        assertThat(databaseServer).isNotNull();
        assertThat(databaseServer.toString()).contains("useSslEnforcement='true'");
    }

    @Test
    void toStringTestWhenEmptyDynamicModel() {
        DatabaseServer databaseServer = DatabaseServer.builder().build();

        assertThat(databaseServer).isNotNull();
        assertThat(databaseServer.toString()).contains("dynamicModel=DynamicModel{parameters={}}");
    }

    @Test
    void toStringTestWhenDynamicModelWithSslCertificateIdentifier() {
        DatabaseServer databaseServer = DatabaseServer.builder().build();
        databaseServer.putParameter(DatabaseServer.SSL_CERTIFICATE_IDENTIFIER, "mycert");

        assertThat(databaseServer).isNotNull();
        assertThat(databaseServer.toString()).contains("dynamicModel=DynamicModel{parameters={sslCertificateIdentifier=mycert}}");
    }

}
