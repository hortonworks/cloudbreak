package com.sequenceiq.cloudbreak.service.kerberos;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.kerberos.KerberosRequest;
import com.sequenceiq.cloudbreak.api.model.kerberos.KerberosTypeBase;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.validation.KerberosRequestValidator;

@Component
public class KerberosTypeResolver {

    public KerberosTypeBase propagateKerberosConfiguration(KerberosRequest request) {
        if (request == null || !KerberosRequestValidator.isValid(request)) {
            throw new BadRequestException("Improper KerberosRequest!");
        }
        KerberosTypeBase kerberos;
        if (request.getAmbariKerberosDescriptor() != null) {
            kerberos = request.getAmbariKerberosDescriptor();
        } else if (request.getFreeIpa() != null) {
            kerberos = request.getFreeIpa();
        } else if (request.getActiveDirectory() != null) {
            kerberos = request.getActiveDirectory();
        } else if (request.getMit() != null) {
            kerberos = request.getMit();
        } else {
            throw new BadRequestException("Unable to determine Kerberos Configuration since none of them are provided in the request!");
        }
        return kerberos;
    }

}
