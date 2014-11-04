package com.sequenceiq.cloudbreak.service.credential.gcc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.LoggerResourceType;
import com.sequenceiq.cloudbreak.service.credential.RsaPublicKeyValidator;
import com.sequenceiq.cloudbreak.service.stack.connector.gcc.GccStackUtil;

@Component
public class GccCredentialInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccCredentialInitializer.class);
    @Autowired
    private RsaPublicKeyValidator rsaPublicKeyValidator;
    @Autowired
    private GccStackUtil gccStackUtil;

    public GccCredential init(GccCredential gccCredential) {
        rsaPublicKeyValidator.validate(gccCredential);
        validateCredential(gccCredential);
        return gccCredential;
    }

    private void validateCredential(GccCredential gccCredential) {
        MDC.put(LoggerContextKey.OWNER_ID.toString(), gccCredential.getOwner());
        MDC.put(LoggerContextKey.RESOURCE_ID.toString(), gccCredential.getId().toString());
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.CREDENTIAL_ID.toString());
        try {
            Compute compute = gccStackUtil.buildCompute(gccCredential, "myapp");
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
