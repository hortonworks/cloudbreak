package com.sequenceiq.cloudbreak.service.credential.gcc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.credential.RsaPublicKeyValidator;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccStackUtil;

@Component
public class GccCredentialHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccCredentialHandler.class);
    @Autowired
    private RsaPublicKeyValidator rsaPublicKeyValidator;
    @Autowired
    private GccStackUtil gccStackUtil;

    public GccCredential init(GccCredential gccCredential) {
        MDCBuilder.buildMdcContext(gccCredential);
        rsaPublicKeyValidator.validate(gccCredential);
        validateCredential(gccCredential);
        return gccCredential;
    }

    private void validateCredential(GccCredential gccCredential) {
        MDCBuilder.buildMdcContext(gccCredential);
        try {
            Compute compute = gccStackUtil.buildCompute(gccCredential);
            if (compute == null) {
                throw new BadRequestException("Problem with your credential key please use the correct format.");
            }
            gccStackUtil.listDisks(compute, gccCredential);
        } catch (Exception e) {
            String errorMessage = String.format("Could not validate credential [credential: '%s'], detailed message: %s",
                    gccCredential.getName(), e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new BadRequestException(errorMessage, e);
        }
    }
}
