package com.sequenceiq.cloudbreak.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DataBusEndpointProviderTest {

    private DataBusEndpointProvider underTest;

    @BeforeEach
    public void setUp() {
        underTest = new DataBusEndpointProvider();
    }

    @Test
    public void testGetDatabusS3EndpointWithProdUrl() {
        // GIVEN
        // WHEN
        String result = underTest.getDatabusS3Endpoint("https://dbusapi.us-west-1.sigma.altus.cloudera.com");
        // THEN
        assertEquals("https://cloudera-dbus-prod.s3.amazonaws.com", result);
    }

    @Test
    public void testGetDatabusS3EndpointWithProdCNameUrl() {
        // GIVEN
        // WHEN
        String result = underTest.getDatabusS3Endpoint("https://dbusapi.us-west-1.altus.cloudera.com");
        // THEN
        assertEquals("https://cloudera-dbus-prod.s3.amazonaws.com", result);
    }

    @Test
    public void testGetDatabusS3EndpointWithDevUrl() {
        // GIVEN
        // WHEN
        String result = underTest.getDatabusS3Endpoint("https://dbusapi.sigma-dev.cloudera.com");
        // THEN
        assertEquals("https://cloudera-dbus-dev.s3.amazonaws.com", result);
    }

    @Test
    public void testGetDatabusS3EndpointWithStageUrl() {
        // GIVEN
        // WHEN
        String result = underTest.getDatabusS3Endpoint("https://dbusapi.sigma-stage.cloudera.com");
        // THEN
        assertEquals("https://cloudera-dbus-stage.s3.amazonaws.com", result);
    }

    @Test
    public void testGetDatabusS3EndpointWithIntUrl() {
        // GIVEN
        // WHEN
        String result = underTest.getDatabusS3Endpoint("https://dbusapi.sigma-int.cloudera.com");
        // THEN
        assertNull(result);
    }

    @Test
    public void testGetDatabusS3EndpointWithWrongUrl() {
        // GIVEN
        // WHEN
        String result = underTest.getDatabusS3Endpoint("badurl");
        // THEN
        assertNull(result);
    }

    @Test
    public void testGetDatabusS3EndpointWithEmptyUrl() {
        // GIVEN
        // WHEN
        String result = underTest.getDatabusS3Endpoint(null);
        // THEN
        assertNull(result);
    }
}
