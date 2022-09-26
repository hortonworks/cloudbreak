package com.sequenceiq.cloudbreak.structuredevent.util;

import static com.sequenceiq.cloudbreak.structuredevent.util.LoggingStream.MAX_CONTENT_LENGTH;
import static java.lang.Boolean.TRUE;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

public class RestFilterRequestBodyLogger {

    private RestFilterRequestBodyLogger() {
    }

    public static InputStream logInboundEntity(StringBuilder outContent, InputStream stream, Charset charset, Boolean contentLogging) throws IOException {
        if (TRUE.equals(contentLogging)) {
            if (!stream.markSupported()) {
                stream = new BufferedInputStream(stream, MAX_CONTENT_LENGTH + 1);
            }
            stream.mark(MAX_CONTENT_LENGTH + 1);
            byte[] entity = new byte[MAX_CONTENT_LENGTH + 1];
            int entitySize = IOUtils.read(stream, entity);
            if (entitySize != -1) {
                outContent.append(new String(entity, 0, Math.min(entitySize, MAX_CONTENT_LENGTH), charset));
                if (entitySize > MAX_CONTENT_LENGTH) {
                    outContent.append("...more...");
                }
            }
            outContent.append('\n');
            stream.reset();
        }
        return stream;
    }
}
