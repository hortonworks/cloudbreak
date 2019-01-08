package com.sequenceiq.cloudbreak.converter.v2;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosTypeResolver;

@Component
public class KerberosRequestToKerberosConfigConverter extends AbstractConversionServiceAwareConverter<KerberosV4Request, KerberosConfig> {

    @Inject
    private KerberosTypeResolver kerberosTypeResolver;

    @Override
    public KerberosConfig convert(KerberosV4Request source) {
        KerberosConfig kerberosConfig = getConversionService().convert(kerberosTypeResolver.propagateKerberosConfiguration(source),
                KerberosConfig.class);
        if (kerberosConfig == null) {
            throw new BadRequestException("Obtaining KerberosConfig from KerberosTypeBase was unsuccessful, it has returned null. "
                    + "Further operations are impossible");
        }
        kerberosConfig.setDescription(source.getDescription());
        kerberosConfig.setName(source.getName());
        return kerberosConfig;
    }

}
