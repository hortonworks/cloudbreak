package com.sequenceiq.cloudbreak.converter.v2;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.kerberos.KerberosRequest;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosTypeResolver;

@Component
public class KerberosRequestToKerberosConfigConverter extends AbstractConversionServiceAwareConverter<KerberosRequest, KerberosConfig> {

    @Inject
    private KerberosTypeResolver kerberosTypeResolver;

    @Override
    public KerberosConfig convert(KerberosRequest source) {
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
