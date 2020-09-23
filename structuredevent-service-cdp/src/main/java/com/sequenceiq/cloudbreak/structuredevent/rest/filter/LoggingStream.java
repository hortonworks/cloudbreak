package com.sequenceiq.cloudbreak.structuredevent.rest.filter;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

class LoggingStream extends FilterOutputStream {

    public static final int MAX_CONTENT_LENGTH = 65535;

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    private final Boolean contentLogging;

    LoggingStream(OutputStream inner, Boolean contentLogging) {
        super(inner);
        this.contentLogging = contentLogging;
    }

    StringBuffer getStringBuilder(Charset charset) {
        StringBuffer b = new StringBuffer();
        if (contentLogging) {
            byte[] entity = baos.toByteArray();
            b.append(new String(entity, 0, Math.min(entity.length, MAX_CONTENT_LENGTH), charset));
            if (entity.length > MAX_CONTENT_LENGTH) {
                b.append("...more...");
            }
            b.append('\n');
        }
        return b;
    }

    @Override
    public void write(int i) throws IOException {
        if (contentLogging && baos.size() <= MAX_CONTENT_LENGTH) {
            baos.write(i);
        }
        out.write(i);
    }
}
