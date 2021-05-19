package com.sequenceiq.cloudbreak.service.blueprint;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Service
public class BlueprintConfigValidator {

    public void validate(Blueprint blueprint) {
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(blueprint.getBlueprintText());
        if (cmTemplateProcessor.isInstantiatorPresent()) {
            throw new BadRequestException("Instantiator is present in your Cloudera Manager template which is probably incorrect.");
        }
        if (cmTemplateProcessor.isRepositoriesPresent()) {
            throw new BadRequestException("Repositories are present in your Cloudera Manager template, this must be removed.");
        }
        Pattern passwordPattern = Pattern.compile("\\*\\*\\*");
        Matcher passwordMatch = passwordPattern.matcher(blueprint.getBlueprintText());
        if (passwordMatch.find()) {
            throw new BadRequestException("Password placeholder with **** is present in your Cloudera Manager template which is probably incorrect.");
        }
        Pattern volumePattern = Pattern.compile("/hadoopfs/fs(.*?)");
        Matcher volumeMatch = volumePattern.matcher(blueprint.getBlueprintText());
        if (volumeMatch.find()) {
            throw new BadRequestException("Volume configuration should not be part of your Cloudera Manager template.");
        }
        if (!cmTemplateProcessor.everyHostTemplateHasRoleConfigGroupsRefNames()) {
            throw new BadRequestException("RoleConfigGroupsRefNames is probably missing or misspelled in your Cloudera Manager template.");
        }
        if (!cmTemplateProcessor.everyServiceHasRoleConfigGroups()) {
            throw new BadRequestException("RoleConfigGroups is probably missing or misspelled in your Cloudera Manager template.");
        }
    }
}
