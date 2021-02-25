package com.sequenceiq.cloudbreak.cloud.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DatabaseServerTest {

    @Test
    void builderTest() {
        assertThat(DatabaseServer.builder()).isNotNull();
    }

    @Test
    void isUseSslEnforcementTestWhenDefault() {
        DatabaseServer databaseServer = DatabaseServer.builder().build();

        assertThat(databaseServer).isNotNull();
        assertThat(databaseServer.isUseSslEnforcement()).isFalse();
    }

    @Test
    void isUseSslEnforcementTestWhenFalse() {
        DatabaseServer databaseServer = DatabaseServer.builder().useSslEnforcement(false).build();

        assertThat(databaseServer).isNotNull();
        assertThat(databaseServer.isUseSslEnforcement()).isFalse();
    }

    @Test
    void isUseSslEnforcementTestWhenTrue() {
        DatabaseServer databaseServer = DatabaseServer.builder().useSslEnforcement(true).build();

        assertThat(databaseServer).isNotNull();
        assertThat(databaseServer.isUseSslEnforcement()).isTrue();
    }

    @Test
    void toStringTestWhenSslEnforcement() {
        DatabaseServer databaseServer = DatabaseServer.builder().useSslEnforcement(true).build();

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