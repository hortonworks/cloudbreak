package com.sequenceiq.cloudbreak.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.config.DataBusS3EndpointPattern;
import com.sequenceiq.cloudbreak.config.DataBusS3EndpointPatternsConfig;

@ExtendWith(MockitoExtension.class)
public class DataBusEndpointProviderTest {

    private static final String DATABUS_ENDPOINT_CNAME = "https://dbusapi.us-west-1.altus.cloudera.com";

    @InjectMocks
    private DataBusEndpointProvider underTest;

    @Mock
    private AltusDatabusConfiguration altusDatabusConfiguration;

    @BeforeEach
    public void setUp() {
        underTest = new DataBusEndpointProvider(altusDatabusConfiguration, createDatabusS3EndpointPatterns(), DATABUS_ENDPOINT_CNAME);
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
    public void testGetDatabusS3EndpointWithProvidedS3Bucket() {
        // GIVEN
        given(altusDatabusConfiguration.getAltusDatabusS3BucketName()).willReturn("mybucket");
        // WHEN
        String result = underTest.getDatabusS3Endpoint("https://dbusapi.us-west-1.sigma.altus.cloudera.com");
        // THEN
        assertEquals("https://mybucket.s3.amazonaws.com", result);
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
        String result = underTest.getDatabusS3Endpoint("https://dbusapi.sigma-doesnotexist.cloudera.com");
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

    private DataBusS3EndpointPatternsConfig createDatabusS3EndpointPatterns() {
        DataBusS3EndpointPatternsConfig dataBusS3EndpointPatternsConfig = new DataBusS3EndpointPatternsConfig();
        List<DataBusS3EndpointPattern> endpointPatterns = new ArrayList<>();
        DataBusS3EndpointPattern s3DevEndpointPattern = new DataBusS3EndpointPattern();
        s3DevEndpointPattern.setPattern("dbusapi.sigma-dev");
        s3DevEndpointPattern.setEndpoint("https://cloudera-dbus-dev.s3.amazonaws.com");
        DataBusS3EndpointPattern s3StageEndpointPattern = new DataBusS3EndpointPattern();
        s3StageEndpointPattern.setPattern("dbusapi.sigma-stage");
        s3StageEndpointPattern.setEndpoint("https://cloudera-dbus-stage.s3.amazonaws.com");
        DataBusS3EndpointPattern s3ProdEndpointPattern = new DataBusS3EndpointPattern();
        s3ProdEndpointPattern.setPattern("dbusapi.us-west-1");
        s3ProdEndpointPattern.setEndpoint("https://cloudera-dbus-prod.s3.amazonaws.com");
        endpointPatterns.add(s3DevEndpointPattern);
        endpointPatterns.add(s3StageEndpointPattern);
        endpointPatterns.add(s3ProdEndpointPattern);
        dataBusS3EndpointPatternsConfig.setPatterns(endpointPatterns);
        return dataBusS3EndpointPatternsConfig;
    }
}
