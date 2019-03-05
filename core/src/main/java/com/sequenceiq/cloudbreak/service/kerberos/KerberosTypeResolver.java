package com.sequenceiq.cloudbreak.service.kerberos;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosTypeBase;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.validation.KerberosRequestValidator;

@Component
public class KerberosTypeResolver {

    public KerberosTypeBase propagateKerberosConfiguration(KerberosV4Request request) {
        if (request == null || !KerberosRequestValidator.isValid(request)) {
            throw new BadRequestException("Improper KerberosV4Request!");
        }
        KerberosTypeBase kerberos;
        if (request.getAmbariDescriptor() != null) {
            kerberos = request.getAmbariDescriptor();
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
