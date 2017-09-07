package com.sequenceiq.cloudbreak.shell.commands.common;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.google.common.base.Splitter;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.api.model.RecoveryMode;
import com.sequenceiq.cloudbreak.shell.completion.HostGroup;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.HostgroupEntry;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class HostGroupCommands implements CommandMarker {

    private ShellContext shellContext;

    public HostGroupCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @CliAvailabilityIndicator("hostgroup configure")
    public boolean isCreateHostGroupAvailable() {
        return (shellContext.isBlueprintAvailable() && shellContext.isCredentialAvailable() || shellContext.isStackAvailable())
                && !shellContext.isMarathonMode()
                && !shellContext.isYarnMode();
    }

    @CliAvailabilityIndicator("hostgroup show")
    public boolean isShowHostGroupAvailable() {
        return !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @CliCommand(value = "hostgroup configure", help = "Configure host groups")
    public String createHostGroup(
            @CliOption(key = "hostgroup", mandatory = true, help = "Name of the hostgroup") HostGroup hostgroup,
            @CliOption(key = "recipeIds", help = "A comma separated list of recipe ids") String recipeIds,
            @CliOption(key = "recipeNames", help = "A comma separated list of recipe names") String recipeNames,
            @CliOption(key = "recoveryMode", help = "The recovery mode: AUTO or MANUAL", unspecifiedDefaultValue = "MANUAL") RecoveryMode recoverMode) {
        try {
            Set<Long> recipeIdSet = new HashSet<>();
            if (recipeIds != null) {
                recipeIdSet.addAll(getRecipeIds(recipeIds, RecipeParameterType.ID));
            }
            if (recipeNames != null) {
                recipeIdSet.addAll(getRecipeIds(recipeNames, RecipeParameterType.NAME));
            }
            if (shellContext.getInstanceGroups().get(hostgroup.getName()) == null) {
                throw shellContext.exceptionTransformer().transformToRuntimeException(String.format("Instancegroup named %s is missing", hostgroup.getName()));
            }
            shellContext.putHostGroup(hostgroup.getName(),
                    new HostgroupEntry(shellContext.getInstanceGroups().get(hostgroup.getName()).getNodeCount(), recipeIdSet, recoverMode));
            if (shellContext.getHostGroups().entrySet().size() == shellContext.getInstanceGroups().entrySet().size()) {
                shellContext.setHint(Hints.CREATE_CLUSTER);
            } else {
                shellContext.setHint(Hints.CONFIGURE_HOSTGROUP);
            }
            return shellContext.outputTransformer().render(shellContext.getHostGroups(), "hostgroup");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "hostgroup show", help = "Show the currently available host groups")
    public String showHostGroup() throws Exception {
        return shellContext.outputTransformer().render(shellContext.getHostGroups(), "hostgroup");
    }

    private enum RecipeParameterType {
        ID, NAME
    }

    private Set<Long> getRecipeIds(String inputs, RecipeParameterType type) {
        return StreamSupport.stream(Splitter.on(",").omitEmptyStrings().trimResults().split(inputs).spliterator(), false).map(input -> {
            try {
                RecipeResponse resp;
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
            } catch (RuntimeException e) {
                throw new RuntimeException(e.getMessage());
            }
        }).collect(Collectors.toSet());
    }
}
