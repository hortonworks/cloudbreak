package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.cloudbreak.domain.stack.DnsResolverType;

public class DnsResolverTypeConverter extends DefaultEnumConverter<DnsResolverType>  {
    @Override
    public DnsResolverType getDefault() {
        return DnsResolverType.UNKNOWN;
    }
}
