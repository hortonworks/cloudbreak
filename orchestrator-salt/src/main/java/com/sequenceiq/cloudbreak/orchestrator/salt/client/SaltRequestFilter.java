package com.sequenceiq.cloudbreak.orchestrator.salt.client;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.Form;

import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;

public class SaltRequestFilter implements ClientRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltRequestFilter.class);

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        StringBuilder logMessage = new StringBuilder("[Salt Request]:")
                .append("\nurl: ")
                .append(requestContext.getUri().getPath())
                .append("\nmethod: ")
                .append(requestContext.getMethod())
                .append("\nbody: ");

        try {
            Object entity = requestContext.getEntity();
            if (entity != null) {
                if (entity instanceof Form form) {
                    logMessage.append("\nform: ").append(AnonymizerUtil.anonymize(String.valueOf(form.asMap())));
                } else if (entity instanceof FormDataMultiPart formDataMultiPart) {
                    logMessage.append("\nmultipart: ");
                    for (BodyPart bodyPart : formDataMultiPart.getBodyParts()) {
                        if (bodyPart instanceof FormDataBodyPart formDataBodyPart) {
                            if (bodyPart instanceof StreamDataBodyPart streamDataBodyPart) {
                                streamDataBodyPart.getFileName().ifPresent(fileName ->
                                    logMessage.append("\n file-name: ").append(fileName));
                            } else {
                                logMessage.append("\n form-data: ")
                                        .append(AnonymizerUtil.anonymize(formDataBodyPart.getContentDisposition().toString()))
                                        .append("\n entity: ")
                                        .append(AnonymizerUtil.anonymize(formDataBodyPart.getEntity().toString()));
                            }
                        }
                    }
                } else {
                    logMessage.append(AnonymizerUtil.anonymize(entity.toString()));
                }
            } else {
                logMessage.append("<empty>");
            }
        } catch (Exception e) {
            logMessage.append("<body mapping exception: ").append(e.getMessage()).append(">");
        }

        LOGGER.info(logMessage.toString());
    }
}
