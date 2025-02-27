package com.sequenceiq.cloudbreak.orchestrator.salt.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;

import org.glassfish.jersey.message.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;

public class SaltReaderInterceptor implements ReaderInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltReaderInterceptor.class);

    private static final int MAX_LOG_LENGTH = 4096;

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        if (!LOGGER.isDebugEnabled()) {
            return context.proceed();
        }

        InputStream originalInputStream = context.getInputStream();
        byte[] responseBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); BufferedInputStream bis = new BufferedInputStream(originalInputStream)) {
            bis.transferTo(baos);
            responseBytes = baos.toByteArray();
        }
        context.setInputStream(new BufferedInputStream(new ByteArrayInputStream(responseBytes)));
        String responseBody = new String(responseBytes, MessageUtils.getCharset(context.getMediaType()));
        if (responseBody.length() > MAX_LOG_LENGTH) {
            LOGGER.debug("[Salt Response Body]:\n{}...\n[TRUNCATED: Original response was {} characters]",
                    AnonymizerUtil.anonymize(responseBody.substring(0, MAX_LOG_LENGTH)), responseBody.length());
        } else {
            LOGGER.debug("[Salt Response Body]:\n{}",  AnonymizerUtil.anonymize(responseBody));
        }

        return context.proceed();
    }
}
