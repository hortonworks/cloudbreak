package com.sequenceiq.it.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import com.sequenceiq.cloudbreak.audit.AuditClient;
import com.sequenceiq.cloudbreak.structuredevent.rest.LegacyRestCommonService;
import com.sequenceiq.cloudbreak.structuredevent.rest.LegacyStructuredEventFilter;

@Configuration()
@ComponentScan(basePackages = {
        "com.sequenceiq.cloudbreak.structuredevent.conf",
        "com.sequenceiq.cloudbreak.audit.config",
        "com.sequenceiq.cloudbreak.audit.converter",
        "com.sequenceiq.cloudbreak.audit.model",
        "com.sequenceiq.cloudbreak.audit.util"},
        basePackageClasses = {AuditClient.class, LegacyRestCommonService.class},
        excludeFilters = @ComponentScan.Filter(
                classes = LegacyStructuredEventFilter.class,
                type = FilterType.ASSIGNABLE_TYPE
        )
)
public class AuditBeanConfig {

}
