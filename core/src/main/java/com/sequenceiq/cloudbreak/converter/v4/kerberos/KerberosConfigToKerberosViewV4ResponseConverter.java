package com.sequenceiq.cloudbreak.converter.v4.kerberos;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosViewV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@Component
public class KerberosConfigToKerberosViewV4ResponseConverter extends AbstractConversionServiceAwareConverter<KerberosConfig, KerberosViewV4Response> {

    @Override
    public KerberosViewV4Response convert(KerberosConfig source) {
        KerberosViewV4Response view = new KerberosViewV4Response();
        view.setName(source.getName());
        view.setType(source.getType());
        view.setId(source.getId());
        view.setDescription(source.getDescription());
        return view;
    }

}
