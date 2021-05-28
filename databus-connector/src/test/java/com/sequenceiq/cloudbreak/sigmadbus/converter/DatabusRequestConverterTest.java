package com.sequenceiq.cloudbreak.sigmadbus.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.cloudera.sigma.service.dbus.DbusProto;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequest;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequestContext;
import com.sequenceiq.cloudbreak.telemetry.metering.MeteringConfiguration;

public class DatabusRequestConverterTest {

    @Test
    public void testConvert() {
        // GIVEN
        MeteringConfiguration meteringConfiguration = new MeteringConfiguration(true, "CdpVmMetrics", "CdpVmMetrics");
        DatabusRequest input = DatabusRequest.Builder.newBuilder()
                .withRawBody("{}")
                .withContext(DatabusRequestContext.Builder.newBuilder()
                        .withAccountId("cloudera")
                        .addAdditionalDatabusHeader("third-header", "app-name")
                        .build())
                .build();
        // WHEN
        DbusProto.PutRecordRequest result = DatabusRequestConverter.convert(input, meteringConfiguration);
        // THEN
        assertEquals("cloudera", result.getRecord().getAccountId());
        assertEquals("{}", result.getRecord().getBody().getPayload().toStringUtf8());
        assertEquals(3, result.getRecord().getBody().getHeaderCount());
    }

    @Test
    public void testConvertWithoutPayload() {
        // GIVEN
        MeteringConfiguration meteringConfiguration = new MeteringConfiguration(true, "CdpVmMetrics", "CdpVmMetrics");
        DatabusRequest input = DatabusRequest.Builder.newBuilder()
                .withContext(DatabusRequestContext.Builder.newBuilder()
                        .withAccountId("cloudera")
                        .build())
                .build();
        // WHEN
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> DatabusRequestConverter.convert(input, meteringConfiguration));
        // THEN
        assertTrue(exception.getMessage().contains("At least raw body message needs to be filled"));
    }

    @Test
    public void testConvertWithoutAccountId() {
        // GIVEN
        MeteringConfiguration meteringConfiguration = new MeteringConfiguration(true, "CdpVmMetrics", "CdpVmMetrics");
        DatabusRequest input = DatabusRequest.Builder.newBuilder()
                .withRawBody("{}")
                .withContext(DatabusRequestContext.Builder.newBuilder()
                        .addAdditionalDatabusHeader("third-header", "app-name")
                        .build())
                .build();
        // WHEN
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> DatabusRequestConverter.convert(input, meteringConfiguration));
        // THEN
        assertTrue(exception.getMessage().contains("At least accountId needs to be filled"));
    }

    @Test
    public void testConvertWithoutContext() {
        // GIVEN
        MeteringConfiguration meteringConfiguration = new MeteringConfiguration(true, "CdpVmMetrics", "CdpVmMetrics");
        DatabusRequest input = DatabusRequest.Builder.newBuilder()
                .withRawBody("{}")
                .build();
        // WHEN
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> DatabusRequestConverter.convert(input, meteringConfiguration));
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
        MeteringConfiguration meteringConfiguration = new MeteringConfiguration(true, "CdpVmMetrics", "CdpVmMetrics");
        DatabusRequest input = DatabusRequest.Builder.newBuilder()
                .withMessageBody(exampleProtoObj)
                .withContext(DatabusRequestContext.Builder.newBuilder()
                        .withAccountId("cloudera")
                        .build())
                .build();
        // WHEN
        DbusProto.PutRecordRequest result = DatabusRequestConverter.convert(input, meteringConfiguration);
        // THEN
        assertTrue(result.getRecord().getBody().getPayload().toStringUtf8().contains("example"));
    }
}
