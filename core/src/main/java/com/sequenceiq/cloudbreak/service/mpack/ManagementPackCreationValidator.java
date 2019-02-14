package com.sequenceiq.cloudbreak.service.mpack;

import javax.annotation.PostConstruct;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.controller.validation.Validator;
import com.sequenceiq.cloudbreak.domain.ManagementPack;

@Component
public class ManagementPackCreationValidator implements Validator<ManagementPack> {

    private static final String MEDIA_TYPE_APPLICATION = "application";

    private static final String MEDIA_SUB_TYPE_X_TAR = "x-tar";

    private static final String MEDIA_SUB_TYPE_OCTET_STREAM = "octet-stream";

    private Client client;

    @PostConstruct
    public void init() {
        client = RestClientUtil.get(new ConfigKey(true, false, false));
    }

    @Override
    public ValidationResult validate(ManagementPack subject) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        if (!subject.isIgnoreValidation()) {
            WebTarget target = client.target(subject.getMpackUrl());
            try (Response response = target.request().head()) {
                if (response.getStatusInfo() != null) {
                    if (!response.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
                        resultBuilder.error(String.format("The URL is invalid! Response code was ['%s'].", response.getStatus()));
                    }
                }
                if (response.getMediaType() != null) {
                    if (!isMediaTypeValid(response.getMediaType())) {
                        resultBuilder.error(String.format("Media type ['%s'] is invalid. It should be ['%s' or '%s'].", response.getMediaType(),
                                MEDIA_TYPE_APPLICATION + '/' + MEDIA_SUB_TYPE_X_TAR, MEDIA_TYPE_APPLICATION + '/' + MEDIA_SUB_TYPE_OCTET_STREAM));
                    }
                }
            }
        }
        return resultBuilder.build();
    }

    private boolean isMediaTypeValid(MediaType mediaType) {
        return MEDIA_TYPE_APPLICATION.equalsIgnoreCase(mediaType.getType())
                && (MEDIA_SUB_TYPE_X_TAR.equalsIgnoreCase(mediaType.getSubtype()) || MEDIA_SUB_TYPE_OCTET_STREAM.equalsIgnoreCase(mediaType.getSubtype()));
    }
}
