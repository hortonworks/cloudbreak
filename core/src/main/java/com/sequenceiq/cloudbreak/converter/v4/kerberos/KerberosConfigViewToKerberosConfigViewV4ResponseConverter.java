package com.sequenceiq.cloudbreak.converter.v4.kerberos;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosViewV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@Component
public class KerberosConfigViewToKerberosConfigViewV4ResponseConverter extends AbstractConversionServiceAwareConverter<KerberosConfig,
        KerberosViewV4Response> {

    @Override
    public KerberosViewV4Response convert(KerberosConfig entity) {
        KerberosViewV4Response response = new KerberosViewV4Response();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setType(entity.getType());
        response.setDescription(entity.getDescription());
        return response;
    }

}
