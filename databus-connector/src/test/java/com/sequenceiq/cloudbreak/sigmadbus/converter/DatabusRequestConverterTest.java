package com.sequenceiq.cloudbreak.sigmadbus.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.cloudera.sigma.service.dbus.DbusProto;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequest;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequestContext;
import com.sequenceiq.cloudbreak.telemetry.support.SupportBundleConfiguration;

public class DatabusRequestConverterTest {

    private static final SupportBundleConfiguration SUPPORT_BUNDLE_CONF = new SupportBundleConfiguration(true, "", "", true);

    @Test
    public void testConvert() {
        // GIVEN
        DatabusRequest input = DatabusRequest.Builder.newBuilder()
                .withRawBody("{}")
                .withContext(DatabusRequestContext.Builder.newBuilder()
                        .withAccountId("cloudera")
                        .addAdditionalDatabusHeader("third-header", "app-name")
                        .build())
                .build();
        // WHEN
        DbusProto.PutRecordRequest result = DatabusRequestConverter.convert(input, SUPPORT_BUNDLE_CONF);
        // THEN
        assertEquals("cloudera", result.getRecord().getAccountId());
        assertEquals("{}", result.getRecord().getBody().getPayload().toStringUtf8());
        assertEquals(3, result.getRecord().getBody().getHeaderCount());
    }

    @Test
    public void testConvertWithoutPayload() {
        // GIVEN
        DatabusRequest input = DatabusRequest.Builder.newBuilder()
                .withContext(DatabusRequestContext.Builder.newBuilder()
                        .withAccountId("cloudera")
                        .build())
                .build();
        // WHEN
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> DatabusRequestConverter.convert(input, SUPPORT_BUNDLE_CONF));
        // THEN
        assertTrue(exception.getMessage().contains("At least raw body message needs to be filled"));
    }

    @Test
    public void testConvertWithoutAccountId() {
        // GIVEN
        DatabusRequest input = DatabusRequest.Builder.newBuilder()
                .withRawBody("{}")
                .withContext(DatabusRequestContext.Builder.newBuilder()
                        .addAdditionalDatabusHeader("third-header", "app-name")
                        .build())
                .build();
        // WHEN
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> DatabusRequestConverter.convert(input, SUPPORT_BUNDLE_CONF));
        // THEN
        assertTrue(exception.getMessage().contains("At least accountId needs to be filled"));
    }

    @Test
    public void testConvertWithoutContext() {
        // GIVEN
        DatabusRequest input = DatabusRequest.Builder.newBuilder()
                .withRawBody("{}")
                .build();
        // WHEN
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> DatabusRequestConverter.convert(input, SUPPORT_BUNDLE_CONF));
        // THEN
        assertTrue(exception.getMessage().contains("Databus request context needs to be filled"));
    }

    @Test
    public void testConvertWithGrpcBody() {
        // GIVEN
        DbusProto.PutRecordRequest exampleProtoObj = DbusProto.PutRecordRequest.newBuilder()
                .setRecord(DbusProto.Record.newBuilder()
                        .setStreamName("example")
                        .build())
                .build();
        DatabusRequest input = DatabusRequest.Builder.newBuilder()
                .withMessageBody(exampleProtoObj)
                .withContext(DatabusRequestContext.Builder.newBuilder()
                        .withAccountId("cloudera")
                        .build())
                .build();
        // WHEN
        DbusProto.PutRecordRequest result = DatabusRequestConverter.convert(input, SUPPORT_BUNDLE_CONF);
        // THEN
        assertTrue(result.getRecord().getBody().getPayload().toStringUtf8().contains("example"));
    }
}
