package com.sequenceiq.cloudbreak.shell.configuration;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.shell.commands.base.BaseCredentialCommands;
import com.sequenceiq.cloudbreak.shell.commands.base.BaseNetworkCommands;
import com.sequenceiq.cloudbreak.shell.commands.base.BasePlatformCommands;
import com.sequenceiq.cloudbreak.shell.commands.base.BaseStackCommands;
import com.sequenceiq.cloudbreak.shell.commands.base.BaseTemplateCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.BasicCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.BlueprintCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.ClusterCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.DatabaseCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.HostGroupCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.InstanceGroupCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.RdsConfigCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.LdapConfigCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.RecipeCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.SecurityGroupCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.SssdConfigCommands;
import com.sequenceiq.cloudbreak.shell.commands.provider.AwsCommands;
import com.sequenceiq.cloudbreak.shell.commands.provider.AzureCommands;
import com.sequenceiq.cloudbreak.shell.commands.provider.GcpCommands;
import com.sequenceiq.cloudbreak.shell.commands.provider.OpenStackCommands;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.util.CloudbreakShellUtil;

@Configuration
public class CommandDefinition {

    @Inject
    private ShellContext shellContext;
    @Inject
    private CloudbreakShellUtil cloudbreakShellUtil;

    @Bean
    BaseCredentialCommands baseCredentialCommands() {
        return new BaseCredentialCommands(shellContext);
    }

    @Bean
    BaseTemplateCommands baseTemplateCommands() {
        return new BaseTemplateCommands(shellContext);
    }

    @Bean
    BaseNetworkCommands baseNetworkCommands() {
        return new BaseNetworkCommands(shellContext);
    }

    @Bean
    BasePlatformCommands basePlatformCommands() {
        return new BasePlatformCommands(shellContext);
    }

    @Bean
    BasicCommands basicCommands() {
        return new BasicCommands(shellContext);
    }

    @Bean
    BlueprintCommands blueprintCommands() {
        return new BlueprintCommands(shellContext);
    }

    @Bean
    RecipeCommands recipeCommands() {
        return new RecipeCommands(shellContext);
    }

    @Bean
    SecurityGroupCommands securityGroupCommands() {
        return new SecurityGroupCommands(shellContext);
    }

    @Bean
    SssdConfigCommands sssdConfigCommands() {
        return new SssdConfigCommands(shellContext);
    }

    @Bean
    RdsConfigCommands rdsConfigCommands() {
        return new RdsConfigCommands(shellContext);
    }

    @Bean
    LdapConfigCommands ldapConfigCommands() {
        return new LdapConfigCommands(shellContext);
    }

    @Bean
    BaseStackCommands stackCommands() {
        return new BaseStackCommands(shellContext, cloudbreakShellUtil);
    }

    @Bean
    HostGroupCommands hostGroupCommands() {
        return new HostGroupCommands(shellContext);
    }

    @Bean
    InstanceGroupCommands instanceGroupCommands() {
        return new InstanceGroupCommands(shellContext);
    }

    @Bean
    DatabaseCommands databaseCommands() {
        return new DatabaseCommands(shellContext);
    }

    @Bean
    ClusterCommands clusterCommands() {
        return new ClusterCommands(shellContext, cloudbreakShellUtil);
    }

    @Bean
    public AwsCommands awsCredentialCommands() {
        return new AwsCommands(shellContext, baseCredentialCommands(), baseNetworkCommands(),
                baseTemplateCommands(), basePlatformCommands(), stackCommands());
    }

    @Bean
    public AzureCommands azureCredentialCommands() {
        return new AzureCommands(shellContext, baseCredentialCommands(), baseNetworkCommands(),
                baseTemplateCommands(), basePlatformCommands(), stackCommands());
    }

    @Bean
    public GcpCommands gcpCredentialCommands() {
        return new GcpCommands(shellContext, baseCredentialCommands(), baseNetworkCommands(),
                baseTemplateCommands(), basePlatformCommands(), stackCommands());
    }

    @Bean
    public OpenStackCommands openStackCredentialCommands() {
        return new OpenStackCommands(shellContext, baseCredentialCommands(), baseNetworkCommands(),
                baseTemplateCommands(), basePlatformCommands(), stackCommands());
    }
}
