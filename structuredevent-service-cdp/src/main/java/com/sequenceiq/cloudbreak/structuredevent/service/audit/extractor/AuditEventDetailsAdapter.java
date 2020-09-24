package com.sequenceiq.cloudbreak.structuredevent.service.audit.extractor;

import java.io.IOException;

import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.protobuf.util.JsonFormat;
import com.sequenceiq.cloudbreak.audit.converter.AuditEventDetailsProto;

public class AuditEventDetailsAdapter extends TypeAdapter<AuditEventDetailsProto.AuditEventDetails> {
    @Override
    public void write(JsonWriter jsonWriter, AuditEventDetailsProto.AuditEventDetails auditEventDetails) throws IOException {
        jsonWriter.jsonValue(JsonFormat.printer().print(auditEventDetails));
    }

    @Override
    public AuditEventDetailsProto.AuditEventDetails read(JsonReader jsonReader) throws IOException {
        AuditEventDetailsProto.AuditEventDetails.Builder personBuilder = AuditEventDetailsProto.AuditEventDetails.newBuilder();
        JsonParser jsonParser = new JsonParser();
        JsonFormat.parser().merge(jsonParser.parse(jsonReader).toString(), personBuilder);
        return personBuilder.build();
    }
}
