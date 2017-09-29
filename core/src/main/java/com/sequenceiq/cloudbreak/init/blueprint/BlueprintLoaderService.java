package com.sequenceiq.cloudbreak.init.blueprint;

import static com.sequenceiq.cloudbreak.common.type.ResourceStatus.DEFAULT;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;

@Service
public class BlueprintLoaderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintLoaderService.class);

    @Inject
    private DefaultBlueprintCache defaultBlueprintCache;

    @Inject
    private BlueprintRepository blueprintRepository;

    public boolean addingDefaultBlueprintsAreNecessaryForTheUser(Set<Blueprint> blueprints) {
        for (Blueprint blueprintFromDatabase : blueprints) {
            Blueprint defaultBlueprint = defaultBlueprintCache.defaultBlueprints().get(blueprintFromDatabase.getName());
            if (mustUpdateTheExistingBlueprint(blueprintFromDatabase, defaultBlueprint)) {
                return true;
            }
        }
        if (isNewUserOrDeletedEveryDefaultBlueprint(blueprints)) {
            return true;
        }
        if (defaultBlueprintDoesNotExistInTheDatabase(blueprints)) {
            return true;
        }
        return false;
    }

    public Collection<Blueprint> loadBlueprintsForTheSpecifiedUser(IdentityUser user, Set<Blueprint> blueprints) {
        Set<Blueprint> blueprintsWhichShouldBeUpdate = updateDefaultBlueprints(user, blueprints);
        Set<Blueprint> blueprintsWhichAreMissing = addMissingBlueprints(user, blueprints);
        try {
            blueprintsWhichAreMissing.addAll(blueprintsWhichShouldBeUpdate);
            if (blueprintsWhichAreMissing.size() > 0) {
                return Sets.newHashSet(blueprintRepository.save(blueprintsWhichAreMissing));
            }
        } catch (Exception e) {
            LOGGER.error("Blueprints {} is not available for {} user.", collectNames(blueprintsWhichAreMissing), user.getUsername());
        }
        return Sets.newHashSet();
    }

    private Set<String> collectNames(Set<Blueprint> failedToUpdate) {
        return failedToUpdate.stream().map(Blueprint::getName).collect(Collectors.toSet());
    }

    private Set<Blueprint> addMissingBlueprints(IdentityUser user, Set<Blueprint> blueprints) {
        Set<Blueprint> resultList = new HashSet<>();
        for (String diffBlueprintName : collectDeviationOfExistingBlueprintsAndDefaultBlueprints(blueprints)) {
            Blueprint blueprintFromCache = defaultBlueprintCache.defaultBlueprints().get(diffBlueprintName);
            resultList.add(prepateBlueprint(user, blueprintFromCache, blueprintFromCache));
        }
        return resultList;
    }

    private Set<Blueprint> updateDefaultBlueprints(IdentityUser user, Set<Blueprint> blueprints) {
        Set<Blueprint> resultList = new HashSet<>();
        for (Blueprint blueprintFromDatabase : blueprints) {
            Blueprint newBlueprint = defaultBlueprintCache.defaultBlueprints().get(blueprintFromDatabase.getName());
            if (defaultBlueprintExistInTheCache(newBlueprint) && defaultBlueprintContainsNewTexts(blueprintFromDatabase, newBlueprint)) {
                resultList.add(prepateBlueprint(user, blueprintFromDatabase, newBlueprint));
            }
        }
        return resultList;
    }

    private Blueprint prepateBlueprint(IdentityUser user, Blueprint blueprintFromDatabase, Blueprint newBlueprint) {
        blueprintFromDatabase.setAccount(user.getAccount());
        blueprintFromDatabase.setOwner(user.getUserId());
        blueprintFromDatabase.setPublicInAccount(true);
        blueprintFromDatabase.setStatus(DEFAULT);
        blueprintFromDatabase.setBlueprintText(newBlueprint.getBlueprintText());
        blueprintFromDatabase.setHostGroupCount(newBlueprint.getHostGroupCount());
        blueprintFromDatabase.setInputParameters(newBlueprint.getInputParameters());
        blueprintFromDatabase.setBlueprintName(newBlueprint.getBlueprintName());
        return blueprintFromDatabase;
    }

    private Set<String> collectDeviationOfExistingBlueprintsAndDefaultBlueprints(Set<Blueprint> blueprints) {
        Set<String> diff = new HashSet<>();
        for (Map.Entry<String, Blueprint> stringBlueprintEntry : defaultBlueprintCache.defaultBlueprints().entrySet()) {
            boolean contains = false;
            for (Blueprint blueprint : blueprints) {
                if (isRegisteregBlueprintAndDefaultBlueprintIsTheSame(stringBlueprintEntry, blueprint)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                diff.add(stringBlueprintEntry.getKey());
            }
        }
        return diff;
    }

    private boolean isRegisteregBlueprintAndDefaultBlueprintIsTheSame(Map.Entry<String, Blueprint> stringBlueprintEntry, Blueprint blueprint) {
        return blueprint.getName().equals(stringBlueprintEntry.getKey()) && isDefaultBlueprint(blueprint);
    }

    private boolean isDefaultBlueprint(Blueprint bp) {
        return DEFAULT.equals(bp.getStatus());
    }

    private boolean defaultBlueprintContainsNewTexts(Blueprint bp, Blueprint blueprint) {
        return !bp.getBlueprintText().equals(blueprint.getBlueprintText());
    }

    private boolean isNewUserOrDeletedEveryDefaultBlueprint(Set<Blueprint> blueprints) {
        return blueprints.size() == 0;
    }

    private boolean mustUpdateTheExistingBlueprint(Blueprint blueprintFromDatabase, Blueprint defaultBlueprint) {
        return isDefaultBlueprint(blueprintFromDatabase)
                && defaultBlueprintExistInTheCache(defaultBlueprint)
                && defaultBlueprintContainsNewTexts(blueprintFromDatabase, defaultBlueprint);
    }

    private boolean defaultBlueprintDoesNotExistInTheDatabase(Set<Blueprint> blueprints) {
        return collectDeviationOfExistingBlueprintsAndDefaultBlueprints(blueprints).size() > 0;
    }

    private boolean defaultBlueprintExistInTheCache(Blueprint actualDefaultBlueprint) {
        return actualDefaultBlueprint != null;
    }
}
