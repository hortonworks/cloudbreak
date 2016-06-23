package com.sequenceiq.cloudbreak.shell.configuration

import javax.inject.Inject

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import com.sequenceiq.cloudbreak.shell.commands.base.BaseCredentialCommands
import com.sequenceiq.cloudbreak.shell.commands.base.BaseNetworkCommands
import com.sequenceiq.cloudbreak.shell.commands.base.BasePlatformCommands
import com.sequenceiq.cloudbreak.shell.commands.base.BaseStackCommands
import com.sequenceiq.cloudbreak.shell.commands.base.BaseTemplateCommands
import com.sequenceiq.cloudbreak.shell.commands.common.BasicCommands
import com.sequenceiq.cloudbreak.shell.commands.common.BlueprintCommands
import com.sequenceiq.cloudbreak.shell.commands.common.ClusterCommands
import com.sequenceiq.cloudbreak.shell.commands.common.HostGroupCommands
import com.sequenceiq.cloudbreak.shell.commands.common.InstanceGroupCommands
import com.sequenceiq.cloudbreak.shell.commands.common.RecipeCommands
import com.sequenceiq.cloudbreak.shell.commands.common.SecurityGroupCommands
import com.sequenceiq.cloudbreak.shell.commands.common.SssdConfigCommands
import com.sequenceiq.cloudbreak.shell.commands.provider.AwsCommands
import com.sequenceiq.cloudbreak.shell.commands.provider.AzureCommands
import com.sequenceiq.cloudbreak.shell.commands.provider.GcpCommands
import com.sequenceiq.cloudbreak.shell.commands.provider.OpenStackCommands
import com.sequenceiq.cloudbreak.shell.model.ShellContext
import com.sequenceiq.cloudbreak.shell.util.CloudbreakShellUtil

@Configuration
class CommandDefinition {

    @Inject
    private val shellContext: ShellContext? = null
    @Inject
    private val cloudbreakShellUtil: CloudbreakShellUtil? = null

    @Bean
    internal fun baseCredentialCommands(): BaseCredentialCommands {
        return BaseCredentialCommands(shellContext)
    }

    @Bean
    internal fun baseTemplateCommands(): BaseTemplateCommands {
        return BaseTemplateCommands(shellContext)
    }

    @Bean
    internal fun baseNetworkCommands(): BaseNetworkCommands {
        return BaseNetworkCommands(shellContext)
    }

    @Bean
    internal fun basePlatformCommands(): BasePlatformCommands {
        return BasePlatformCommands(shellContext)
    }

    @Bean
    internal fun basicCommands(): BasicCommands {
        return BasicCommands(shellContext)
    }

    @Bean
    internal fun blueprintCommands(): BlueprintCommands {
        return BlueprintCommands(shellContext)
    }

    @Bean
    internal fun recipeCommands(): RecipeCommands {
        return RecipeCommands(shellContext)
    }

    @Bean
    internal fun securityGroupCommands(): SecurityGroupCommands {
        return SecurityGroupCommands(shellContext)
    }

    @Bean
    internal fun sssdConfigCommands(): SssdConfigCommands {
        return SssdConfigCommands(shellContext)
    }

    @Bean
    internal fun stackCommands(): BaseStackCommands {
        return BaseStackCommands(shellContext, cloudbreakShellUtil)
    }

    @Bean
    internal fun hostGroupCommands(): HostGroupCommands {
        return HostGroupCommands(shellContext)
    }

    @Bean
    internal fun instanceGroupCommands(): InstanceGroupCommands {
        return InstanceGroupCommands(shellContext)
    }

    @Bean
    internal fun clusterCommands(): ClusterCommands {
        return ClusterCommands(shellContext, cloudbreakShellUtil)
    }


    @Bean
    fun awsCredentialCommands(): AwsCommands {
        return AwsCommands(shellContext, baseCredentialCommands(), baseNetworkCommands(),
                baseTemplateCommands(), basePlatformCommands(), stackCommands())
    }

    @Bean
    fun azureCredentialCommands(): AzureCommands {
        return AzureCommands(shellContext, baseCredentialCommands(), baseNetworkCommands(),
                baseTemplateCommands(), basePlatformCommands(), stackCommands())
    }

    @Bean
    fun gcpCredentialCommands(): GcpCommands {
        return GcpCommands(shellContext, baseCredentialCommands(), baseNetworkCommands(),
                baseTemplateCommands(), basePlatformCommands(), stackCommands())
    }

    @Bean
    fun openStackCredentialCommands(): OpenStackCommands {
        return OpenStackCommands(shellContext, baseCredentialCommands(), baseNetworkCommands(),
                baseTemplateCommands(), basePlatformCommands(), stackCommands())
    }
}
