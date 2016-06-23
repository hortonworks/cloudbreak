package com.sequenceiq.cloudbreak.shell.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.shell.converters.AvailableCommandsConverter
import org.springframework.shell.converters.BigDecimalConverter
import org.springframework.shell.converters.BigIntegerConverter
import org.springframework.shell.converters.BooleanConverter
import org.springframework.shell.converters.CharacterConverter
import org.springframework.shell.converters.DateConverter
import org.springframework.shell.converters.DoubleConverter
import org.springframework.shell.converters.EnumConverter
import org.springframework.shell.converters.FloatConverter
import org.springframework.shell.converters.IntegerConverter
import org.springframework.shell.converters.LocaleConverter
import org.springframework.shell.converters.LongConverter
import org.springframework.shell.converters.ShortConverter
import org.springframework.shell.converters.SimpleFileConverter
import org.springframework.shell.converters.StaticFieldConverterImpl
import org.springframework.shell.converters.StringConverter
import org.springframework.shell.core.Converter

import com.sequenceiq.cloudbreak.shell.converter.AwsInstanceTypeConverter
import com.sequenceiq.cloudbreak.shell.converter.AwsOrchestratorTypeConverter
import com.sequenceiq.cloudbreak.shell.converter.AwsVolumeTypeConverter
import com.sequenceiq.cloudbreak.shell.converter.AzureInstanceTypeConverter
import com.sequenceiq.cloudbreak.shell.converter.AzureOrchestratorTypeConverter
import com.sequenceiq.cloudbreak.shell.converter.AzureVolumeTypeConverter
import com.sequenceiq.cloudbreak.shell.converter.ConstraintNameConverter
import com.sequenceiq.cloudbreak.shell.converter.GcpInstanceTypeConverter
import com.sequenceiq.cloudbreak.shell.converter.GcpOrchestratorTypeConverter
import com.sequenceiq.cloudbreak.shell.converter.GcpVolumeTypeConverter
import com.sequenceiq.cloudbreak.shell.converter.HostGroupConverter
import com.sequenceiq.cloudbreak.shell.converter.InstanceGroupConverter
import com.sequenceiq.cloudbreak.shell.converter.InstanceGroupTemplateIdConverter
import com.sequenceiq.cloudbreak.shell.converter.InstanceGroupTemplateNameConverter
import com.sequenceiq.cloudbreak.shell.converter.NetworkIdConverter
import com.sequenceiq.cloudbreak.shell.converter.NetworkNameConverter
import com.sequenceiq.cloudbreak.shell.converter.OpenStackFacingConverter
import com.sequenceiq.cloudbreak.shell.converter.OpenStackOrchestratorTypeConverter
import com.sequenceiq.cloudbreak.shell.converter.PlatformVariantConverter
import com.sequenceiq.cloudbreak.shell.converter.PluginExecutionTypeConverter
import com.sequenceiq.cloudbreak.shell.converter.SecurityGroupIdConverter
import com.sequenceiq.cloudbreak.shell.converter.SecurityGroupNameConverter
import com.sequenceiq.cloudbreak.shell.converter.SecurityRulesConverter
import com.sequenceiq.cloudbreak.shell.converter.SssdProviderTypeConverter
import com.sequenceiq.cloudbreak.shell.converter.SssdSchemaTypeConverter
import com.sequenceiq.cloudbreak.shell.converter.SssdTlsReqcertTypeConverter
import com.sequenceiq.cloudbreak.shell.converter.StackAvailabilityZoneConverter
import com.sequenceiq.cloudbreak.shell.converter.StackRegionConverter

/**
 * Configures the converters used by the shell.
 */
@Configuration
class ConverterConfiguration {

    @Bean
    internal fun simpleFileConverter(): Converter<Any> {
        return SimpleFileConverter()
    }

    @Bean
    internal fun stringConverter(): Converter<Any> {
        return StringConverter()
    }

    @Bean
    internal fun availableCommandsConverter(): Converter<Any> {
        return AvailableCommandsConverter()
    }

    @Bean
    internal fun bigDecimalConverter(): Converter<Any> {
        return BigDecimalConverter()
    }

    @Bean
    internal fun bigIntegerConverter(): Converter<Any> {
        return BigIntegerConverter()
    }

    @Bean
    internal fun booleanConverter(): Converter<Any> {
        return BooleanConverter()
    }

    @Bean
    internal fun characterConverter(): Converter<Any> {
        return CharacterConverter()
    }

    @Bean
    internal fun dateConverter(): Converter<Any> {
        return DateConverter()
    }

    @Bean
    internal fun doubleConverter(): Converter<Any> {
        return DoubleConverter()
    }

    @Bean
    internal fun enumConverter(): Converter<Any> {
        return EnumConverter()
    }

    @Bean
    internal fun floatConverter(): Converter<Any> {
        return FloatConverter()
    }

    @Bean
    internal fun integerConverter(): Converter<Any> {
        return IntegerConverter()
    }

    @Bean
    internal fun localeConverter(): Converter<Any> {
        return LocaleConverter()
    }

    @Bean
    internal fun longConverter(): Converter<Any> {
        return LongConverter()
    }

    @Bean
    internal fun shortConverter(): Converter<Any> {
        return ShortConverter()
    }

    @Bean
    internal fun hostGroupConverter(): Converter<Any> {
        return HostGroupConverter()
    }

    @Bean
    internal fun instanceGroupConverter(): Converter<Any> {
        return InstanceGroupConverter()
    }

    @Bean
    internal fun templateIdConverter(): Converter<Any> {
        return InstanceGroupTemplateIdConverter()
    }

    @Bean
    internal fun templateNameConverter(): Converter<Any> {
        return InstanceGroupTemplateNameConverter()
    }

    @Bean
    internal fun stackRegionConverter(): Converter<Any> {
        return StackRegionConverter()
    }

    @Bean
    internal fun stackAvailabilityZoneConverter(): Converter<Any> {
        return StackAvailabilityZoneConverter()
    }

    @Bean
    internal fun staticFieldConverterImpl(): Converter<Any> {
        return StaticFieldConverterImpl()
    }

    @Bean
    internal fun networkIdConverter(): Converter<Any> {
        return NetworkIdConverter()
    }

    @Bean
    internal fun networkNameConverter(): Converter<Any> {
        return NetworkNameConverter()
    }

    @Bean
    internal fun secGroupIdConverter(): Converter<Any> {
        return SecurityGroupIdConverter()
    }

    @Bean
    internal fun secGroupNameConverter(): Converter<Any> {
        return SecurityGroupNameConverter()
    }

    @Bean
    internal fun platformVariantConverter(): Converter<Any> {
        return PlatformVariantConverter()
    }

    @Bean
    internal fun securityRulesConverter(): Converter<Any> {
        return SecurityRulesConverter()
    }

    @Bean
    internal fun gcpVolumeTypeConverter(): Converter<Any> {
        return GcpVolumeTypeConverter()
    }

    @Bean
    internal fun awsVolumeTypeConverter(): Converter<Any> {
        return AwsVolumeTypeConverter()
    }

    @Bean
    internal fun gcpInstanceTypeConverter(): Converter<Any> {
        return GcpInstanceTypeConverter()
    }

    @Bean
    internal fun awsInstanceTypeConverter(): Converter<Any> {
        return AwsInstanceTypeConverter()
    }

    @Bean
    internal fun azureInstanceTypeConverter(): Converter<Any> {
        return AzureInstanceTypeConverter()
    }

    @Bean
    internal fun azureVolumeTypeConverter(): Converter<Any> {
        return AzureVolumeTypeConverter()
    }

    internal val pluginExecutionTypeConverter: Converter<Any>
        @Bean
        get() = PluginExecutionTypeConverter()

    internal val sssdProviderTypeConverter: Converter<Any>
        @Bean
        get() = SssdProviderTypeConverter()

    internal val sssdSchemaTypeConverter: Converter<Any>
        @Bean
        get() = SssdSchemaTypeConverter()

    internal val openStackFacingConverter: Converter<Any>
        @Bean
        get() = OpenStackFacingConverter()

    internal val sssdTlsReqcertTypeConverter: Converter<Any>
        @Bean
        get() = SssdTlsReqcertTypeConverter()

    @Bean
    internal fun constraintNameConverter(): Converter<Any> {
        return ConstraintNameConverter()
    }

    @Bean
    internal fun openStackOrchestratorTypeConverter(): Converter<Any> {
        return OpenStackOrchestratorTypeConverter()
    }

    @Bean
    internal fun gcpOrchestratorTypeConverter(): Converter<Any> {
        return GcpOrchestratorTypeConverter()
    }

    @Bean
    internal fun awsOrchestratorTypeConverter(): Converter<Any> {
        return AwsOrchestratorTypeConverter()
    }

    @Bean
    internal fun azureOrchestratorTypeConverter(): Converter<Any> {
        return AzureOrchestratorTypeConverter()
    }
}
