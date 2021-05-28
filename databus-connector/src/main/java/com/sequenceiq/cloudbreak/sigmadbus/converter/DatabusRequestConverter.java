package com.sequenceiq.cloudbreak.sigmadbus.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloudera.sigma.service.dbus.DbusProto;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequest;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequestContext;
import com.sequenceiq.cloudbreak.telemetry.databus.AbstractDatabusStreamConfiguration;

public class DatabusRequestConverter {

    private static final long ONE_MB_BYTES = 1024 * 1024;

    private DatabusRequestConverter() {
    }

    public static <D extends AbstractDatabusStreamConfiguration> DbusProto.PutRecordRequest convert(DatabusRequest request, D databusStreamConfiguration) {
        final String payload = getPayload(request);
        final DatabusRequestContext context = getContext(request);
        final int payloadSize = payload.length();
        DbusProto.Record.Body.Builder recordBuilder = DbusProto.Record.Body.newBuilder();
        if (payloadSize < ONE_MB_BYTES) {
            recordBuilder.setPayload(ByteString.copyFromUtf8(payload));
        }
        DbusProto.Record.Body body = recordBuilder.setPayloadSize(payloadSize)
                .addAllHeader(createHeaders(context, databusStreamConfiguration))
                .build();
        DbusProto.Record record =
                DbusProto.Record.newBuilder()
                        .setBody(body)
                        .setAccountId(context.getAccountId())
                        .setStreamName(databusStreamConfiguration.getDbusStreamName())
                        .setPartitionKey("1")
                        .setStrictSizeCheck(true)
                        .build();
        return DbusProto.PutRecordRequest.newBuilder()
                        .setRecord(record)
                        .build();
    }

    private static <D extends AbstractDatabusStreamConfiguration> Iterable<DbusProto.Record.Header> createHeaders(
            DatabusRequestContext context, D databusStreamConfiguration) {
        List<DbusProto.Record.Header> headers = new ArrayList<>();
        if (MapUtils.isNotEmpty(context.getAdditionalDatabusHeaders())) {
            for (Map.Entry<String, String> entry : context.getAdditionalDatabusHeaders().entrySet()) {
                headers.add(DbusProto.Record.Header.newBuilder().setName(entry.getKey()).setValue(entry.getValue()).build());
            }
        }
        String appName = databusStreamConfiguration.getDbusAppName();
        headers.add(DbusProto.Record.Header.newBuilder().setName("app").setValue(appName).build());
        headers.add(DbusProto.Record.Header.newBuilder().setName(databusStreamConfiguration.getDbusAppNameKey()).setValue(appName).build());
        return headers;
    }

    public static String getPayload(DatabusRequest request) {
        Optional<GeneratedMessageV3> grpcMessage = request.getMessageBody();
        final String payload;
        if (grpcMessage.isEmpty()) {
            Optional<String> rawMessage = request.getRawBody();
            if (rawMessage.isEmpty()) {
                throw new IllegalArgumentException("At least raw body message needs to be filled for databus input record.");
            } else {
                payload = rawMessage.get();
            }
        } else {
            try {
                payload = JsonFormat.printer()
                        .omittingInsignificantWhitespace().print(grpcMessage.get());
            } catch (InvalidProtocolBufferException e) {
                throw new IllegalArgumentException("Error during transforming grpc record to json string", e);
            }
        }
        return payload;
    }

    private static DatabusRequestContext getContext(DatabusRequest request) {
        Optional<DatabusRequestContext> contextOpt = request.getContext();
        if (contextOpt.isEmpty()) {
            throw new IllegalArgumentException("Databus request context needs to be filled.");
        } else {
            DatabusRequestContext context = contextOpt.get();
            if (StringUtils.isBlank(context.getAccountId())) {
                throw new IllegalArgumentException("At least accountId needs to be filled in databus request context.");
            }
            return context;
        }
    }
}
