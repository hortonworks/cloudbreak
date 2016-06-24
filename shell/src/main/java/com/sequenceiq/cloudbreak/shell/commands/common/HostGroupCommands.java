package com.sequenceiq.cloudbreak.shell.commands.common;

import java.util.HashSet;
import java.util.Set;

import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.shell.completion.HostGroup;
import com.sequenceiq.cloudbreak.shell.model.HostgroupEntry;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class HostGroupCommands implements CommandMarker {

    private ShellContext shellContext;

    public HostGroupCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @CliAvailabilityIndicator(value = "hostgroup configure")
    public boolean isCreateHostGroupAvailable() {
        return (shellContext.isBlueprintAvailable() && shellContext.isCredentialAvailable()
                || shellContext.isStackAvailable()) && !shellContext.isMarathonMode();
    }

    @CliAvailabilityIndicator(value = "hostgroup show")
    public boolean isShowHostGroupAvailable() {
        return !shellContext.isMarathonMode();
    }

    @CliCommand(value = "hostgroup configure", help = "Configure host groups")
    public String createHostGroup(
            @CliOption(key = "hostgroup", mandatory = true, help = "Name of the hostgroup") HostGroup hostgroup,
            @CliOption(key = "recipeIds", mandatory = false, help = "A comma separated list of recipe ids") String recipeIds,
            @CliOption(key = "recipeNames", mandatory = false, help = "A comma separated list of recipe names") String recipeNames) {
        try {
            Set<Long> recipeIdSet = new HashSet<>();
            if (recipeIds != null) {
                recipeIdSet.addAll(getRecipeIds(recipeIds, RecipeParameterType.ID));
            }
            if (recipeNames != null) {
                recipeIdSet.addAll(getRecipeIds(recipeNames, RecipeParameterType.NAME));
            }
            shellContext.putHostGroup(hostgroup.getName(),
                    new HostgroupEntry(shellContext.getInstanceGroups().get(hostgroup.getName()).getNodeCount(), recipeIdSet));
            return shellContext.outputTransformer().render(shellContext.getHostGroups(), "hostgroup");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "hostgroup show", help = "Configure host groups")
    public String showHostGroup() throws Exception {
        return shellContext.outputTransformer().render(shellContext.getHostGroups(), "hostgroup");
    }

    private enum RecipeParameterType {
        ID, NAME
    }

    private Set<Long> getRecipeIds(String inputs, final RecipeParameterType type) {
        return FluentIterable.from(Splitter.on(",").omitEmptyStrings().trimResults().split(inputs)).transform(input -> {
            try {
                RecipeResponse resp = null;
                switch (type) {
                    case ID:
                        resp = shellContext.cloudbreakClient().recipeEndpoint().get(Long.valueOf(input));
                        break;
                    case NAME:
                        resp = shellContext.cloudbreakClient().recipeEndpoint().getPublic(input);
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
                return resp.getId();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }).toSet();
    }
}
