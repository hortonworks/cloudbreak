package com.sequenceiq.cloudbreak.shell.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.converters.AvailableCommandsConverter;
import org.springframework.shell.converters.BigDecimalConverter;
import org.springframework.shell.converters.BigIntegerConverter;
import org.springframework.shell.converters.BooleanConverter;
import org.springframework.shell.converters.CharacterConverter;
import org.springframework.shell.converters.DateConverter;
import org.springframework.shell.converters.DoubleConverter;
import org.springframework.shell.converters.EnumConverter;
import org.springframework.shell.converters.FloatConverter;
import org.springframework.shell.converters.IntegerConverter;
import org.springframework.shell.converters.LocaleConverter;
import org.springframework.shell.converters.LongConverter;
import org.springframework.shell.converters.ShortConverter;
import org.springframework.shell.converters.SimpleFileConverter;
import org.springframework.shell.converters.StaticFieldConverterImpl;
import org.springframework.shell.converters.StringConverter;
import org.springframework.shell.core.Converter;

import com.sequenceiq.cloudbreak.shell.converter.AwsInstanceTypeConverter;
import com.sequenceiq.cloudbreak.shell.converter.AwsOrchestratorTypeConverter;
import com.sequenceiq.cloudbreak.shell.converter.AwsVolumeTypeConverter;
import com.sequenceiq.cloudbreak.shell.converter.AzureInstanceTypeConverter;
import com.sequenceiq.cloudbreak.shell.converter.AzureOrchestratorTypeConverter;
import com.sequenceiq.cloudbreak.shell.converter.AzureVolumeTypeConverter;
import com.sequenceiq.cloudbreak.shell.converter.ConstraintNameConverter;
import com.sequenceiq.cloudbreak.shell.converter.DatabaseVendorConverter;
import com.sequenceiq.cloudbreak.shell.converter.GcpInstanceTypeConverter;
import com.sequenceiq.cloudbreak.shell.converter.GcpOrchestratorTypeConverter;
import com.sequenceiq.cloudbreak.shell.converter.GcpVolumeTypeConverter;
import com.sequenceiq.cloudbreak.shell.converter.HostGroupConverter;
import com.sequenceiq.cloudbreak.shell.converter.InstanceGroupConverter;
import com.sequenceiq.cloudbreak.shell.converter.InstanceGroupTemplateIdConverter;
import com.sequenceiq.cloudbreak.shell.converter.InstanceGroupTemplateNameConverter;
import com.sequenceiq.cloudbreak.shell.converter.NetworkIdConverter;
import com.sequenceiq.cloudbreak.shell.converter.NetworkNameConverter;
import com.sequenceiq.cloudbreak.shell.converter.OpenStackFacingConverter;
import com.sequenceiq.cloudbreak.shell.converter.OpenStackOrchestratorTypeConverter;
import com.sequenceiq.cloudbreak.shell.converter.PlatformVariantConverter;
import com.sequenceiq.cloudbreak.shell.converter.RdsConfigIdConverter;
import com.sequenceiq.cloudbreak.shell.converter.RdsConfigNameConverter;
import com.sequenceiq.cloudbreak.shell.converter.SecurityGroupIdConverter;
import com.sequenceiq.cloudbreak.shell.converter.SecurityGroupNameConverter;
import com.sequenceiq.cloudbreak.shell.converter.SecurityRulesConverter;
import com.sequenceiq.cloudbreak.shell.converter.SssdProviderTypeConverter;
import com.sequenceiq.cloudbreak.shell.converter.SssdSchemaTypeConverter;
import com.sequenceiq.cloudbreak.shell.converter.SssdTlsReqcertTypeConverter;
import com.sequenceiq.cloudbreak.shell.converter.StackAvailabilityZoneConverter;
import com.sequenceiq.cloudbreak.shell.converter.StackRegionConverter;

/**
 * Configures the converters used by the shell.
 */
@Configuration
public class ConverterConfiguration {

    @Bean
    Converter simpleFileConverter() {
        return new SimpleFileConverter();
    }

    @Bean
    Converter stringConverter() {
        return new StringConverter();
    }

    @Bean
    Converter availableCommandsConverter() {
        return new AvailableCommandsConverter();
    }

    @Bean
    Converter bigDecimalConverter() {
        return new BigDecimalConverter();
    }

    @Bean
    Converter bigIntegerConverter() {
        return new BigIntegerConverter();
    }

    @Bean
    Converter booleanConverter() {
        return new BooleanConverter();
    }

    @Bean
    Converter characterConverter() {
        return new CharacterConverter();
    }

    @Bean
    Converter dateConverter() {
        return new DateConverter();
    }

    @Bean
    Converter doubleConverter() {
        return new DoubleConverter();
    }

    @Bean
    Converter enumConverter() {
        return new EnumConverter();
    }

    @Bean
    Converter floatConverter() {
        return new FloatConverter();
    }

    @Bean
    Converter integerConverter() {
        return new IntegerConverter();
    }

    @Bean
    Converter localeConverter() {
        return new LocaleConverter();
    }

    @Bean
    Converter longConverter() {
        return new LongConverter();
    }

    @Bean
    Converter shortConverter() {
        return new ShortConverter();
    }

    @Bean
    Converter hostGroupConverter() {
        return new HostGroupConverter();
    }

    @Bean
    Converter instanceGroupConverter() {
        return new InstanceGroupConverter();
    }

    @Bean
    Converter templateIdConverter() {
        return new InstanceGroupTemplateIdConverter();
    }

    @Bean
    Converter templateNameConverter() {
        return new InstanceGroupTemplateNameConverter();
    }

    @Bean
    Converter stackRegionConverter() {
        return new StackRegionConverter();
    }

    @Bean
    Converter stackAvailabilityZoneConverter() {
        return new StackAvailabilityZoneConverter();
    }

    @Bean
    Converter staticFieldConverterImpl() {
        return new StaticFieldConverterImpl();
    }

    @Bean
    Converter networkIdConverter() {
        return new NetworkIdConverter();
    }

    @Bean
    Converter networkNameConverter() {
        return new NetworkNameConverter();
    }

    @Bean
    Converter secGroupIdConverter() {
        return new SecurityGroupIdConverter();
    }

    @Bean
    Converter secGroupNameConverter() {
        return new SecurityGroupNameConverter();
    }

    @Bean
    Converter platformVariantConverter() {
        return new PlatformVariantConverter();
    }

    @Bean
    Converter securityRulesConverter() {
        return new SecurityRulesConverter();
    }

    @Bean
    Converter gcpVolumeTypeConverter() {
        return new GcpVolumeTypeConverter();
    }

    @Bean
    Converter awsVolumeTypeConverter() {
        return new AwsVolumeTypeConverter();
    }

    @Bean
    Converter gcpInstanceTypeConverter() {
        return new GcpInstanceTypeConverter();
    }

    @Bean
    Converter awsInstanceTypeConverter() {
        return new AwsInstanceTypeConverter();
    }

    @Bean
    Converter azureInstanceTypeConverter() {
        return new AzureInstanceTypeConverter();
    }

    @Bean
    Converter azureVolumeTypeConverter() {
        return new AzureVolumeTypeConverter();
    }

    @Bean
    Converter getSssdProviderTypeConverter() {
        return new SssdProviderTypeConverter();
    }

    @Bean
    Converter getRdsIdConverter() {
        return new RdsConfigIdConverter();
    }

    @Bean
    Converter getRdsNameConverter() {
        return new RdsConfigNameConverter();
    }

    @Bean
    Converter getSssdSchemaTypeConverter() {
        return new SssdSchemaTypeConverter();
    }

    @Bean
    Converter getOpenStackFacingConverter() {
        return new OpenStackFacingConverter();
    }

    @Bean
    Converter getSssdTlsReqcertTypeConverter() {
        return new SssdTlsReqcertTypeConverter();
    }

    @Bean
    Converter constraintNameConverter() {
        return new ConstraintNameConverter();
    }

    @Bean
    Converter openStackOrchestratorTypeConverter() {
        return new OpenStackOrchestratorTypeConverter();
    }

    @Bean
    Converter gcpOrchestratorTypeConverter() {
        return new GcpOrchestratorTypeConverter();
    }

    @Bean
    Converter awsOrchestratorTypeConverter() {
        return new AwsOrchestratorTypeConverter();
    }

    @Bean
    Converter azureOrchestratorTypeConverter() {
        return new AzureOrchestratorTypeConverter();
    }

    @Bean
    Converter databaseVendorConverter() {
        return new DatabaseVendorConverter();
    }
}
