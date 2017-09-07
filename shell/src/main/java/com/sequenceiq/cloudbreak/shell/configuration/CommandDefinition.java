package com.sequenceiq.cloudbreak.shell.configuration;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.shell.commands.AccountPreferencesCommands;
import com.sequenceiq.cloudbreak.shell.commands.base.BaseAccountPreferencesCommands;
import com.sequenceiq.cloudbreak.shell.commands.base.BaseCredentialCommands;
import com.sequenceiq.cloudbreak.shell.commands.base.BaseInstanceGroupCommands;
import com.sequenceiq.cloudbreak.shell.commands.base.BaseNetworkCommands;
import com.sequenceiq.cloudbreak.shell.commands.base.BasePlatformCommands;
import com.sequenceiq.cloudbreak.shell.commands.base.BaseSecurityGroupCommands;
import com.sequenceiq.cloudbreak.shell.commands.base.BaseStackCommands;
import com.sequenceiq.cloudbreak.shell.commands.base.BaseTemplateCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.BasicCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.BlueprintCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.ClusterCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.DatabaseCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.FlexSubscriptionCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.HostGroupCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.LdapConfigCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.RdsConfigCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.RecipeCommands;
import com.sequenceiq.cloudbreak.shell.commands.common.SmartSenseSubscriptionCommands;
import com.sequenceiq.cloudbreak.shell.commands.provider.AwsCommands;
import com.sequenceiq.cloudbreak.shell.commands.provider.AzureCommands;
import com.sequenceiq.cloudbreak.shell.commands.provider.GcpCommands;
import com.sequenceiq.cloudbreak.shell.commands.provider.MarathonCommands;
import com.sequenceiq.cloudbreak.shell.commands.provider.OpenStackCommands;
import com.sequenceiq.cloudbreak.shell.commands.provider.YarnCommands;
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
    BaseSecurityGroupCommands baseSecurityGroupCommands() {
        return new BaseSecurityGroupCommands(shellContext);
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
    SmartSenseSubscriptionCommands smartSenseSubscriptionCommands() {
        return new SmartSenseSubscriptionCommands(shellContext);
    }

    @Bean
    FlexSubscriptionCommands flexSubscriptionCommands() {
        return new FlexSubscriptionCommands(shellContext);
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
    BaseInstanceGroupCommands instanceGroupCommands() {
        return new BaseInstanceGroupCommands(shellContext);
    }

    @Bean
    DatabaseCommands databaseCommands() {
        return new DatabaseCommands(shellContext);
    }

    @Bean
    ClusterCommands clusterCommands() {
        return new ClusterCommands(shellContext, cloudbreakShellUtil, stackCommands());
    }

    @Bean
    public AwsCommands awsCredentialCommands() {
        return new AwsCommands(shellContext, baseCredentialCommands(), baseNetworkCommands(), baseSecurityGroupCommands(),
                baseTemplateCommands(), basePlatformCommands(), stackCommands(), instanceGroupCommands());
    }

    @Bean
    public AzureCommands azureCredentialCommands() {
        return new AzureCommands(shellContext, baseCredentialCommands(), baseNetworkCommands(), baseSecurityGroupCommands(),
                baseTemplateCommands(), basePlatformCommands(), stackCommands(), instanceGroupCommands());
    }

    @Bean
    public GcpCommands gcpCredentialCommands() {
        return new GcpCommands(shellContext, baseCredentialCommands(), baseNetworkCommands(), baseSecurityGroupCommands(),
                baseTemplateCommands(), basePlatformCommands(), stackCommands(), instanceGroupCommands());
    }

    @Bean
    public OpenStackCommands openStackCredentialCommands() {
        return new OpenStackCommands(shellContext, baseCredentialCommands(), baseNetworkCommands(), baseSecurityGroupCommands(),
                baseTemplateCommands(), basePlatformCommands(), stackCommands(), instanceGroupCommands());
    }

    @Bean
    public YarnCommands yarnCommands() {
        return new YarnCommands(shellContext, baseCredentialCommands(), stackCommands());
    }

    @Bean
    public MarathonCommands marathonCommands() {
        return new MarathonCommands(shellContext, baseCredentialCommands(), stackCommands());
    }

    @Bean
    public AccountPreferencesCommands accountPreferencesCommands() {
        return new BaseAccountPreferencesCommands(shellContext);
    }
}
