package com.sequenceiq.cloudbreak.init.blueprint;

import static com.sequenceiq.cloudbreak.common.type.ResourceStatus.DEFAULT;

import java.util.Collection;
import java.util.HashMap;
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

    public Set<Blueprint> loadBlueprintsForTheSpecifiedUser(IdentityUser user, Set<Blueprint> blueprints) {
        Set<Blueprint> blueprintsWhichShouldBeUpdate = updateDefaultBlueprints(user, blueprints);
        Set<Blueprint> blueprintsWhichAreMissing = addMissingBlueprints(user, blueprints);
        try {
            LOGGER.info("Prepare Blueprint set for the user '{}' after the modifications.", user.getUserId());
            blueprintsWhichAreMissing.addAll(blueprintsWhichShouldBeUpdate);
            if (blueprintsWhichAreMissing.size() > 0) {
                return Sets.newHashSet(getResultSetFromUpdateAndOriginalBlueprints(blueprints, blueprintsWhichAreMissing));
            }
        } catch (Exception e) {
            LOGGER.error("Blueprints {} is not available for {} user.", collectNames(blueprintsWhichAreMissing), user.getUsername());
        }
        return blueprints;
    }

    private Collection<Blueprint> getResultSetFromUpdateAndOriginalBlueprints(Set<Blueprint> blueprints, Set<Blueprint> blueprintsWhichAreMissing) {
        LOGGER.info("Updating blueprints which should be modified.");
        Iterable<Blueprint> savedBlueprints = blueprintRepository.save(blueprintsWhichAreMissing);
        LOGGER.info("Finished to update blueprints which should be modified.");
        Map<String, Blueprint> resultBlueprints = new HashMap<>();
        for (Blueprint blueprint : blueprints) {
            resultBlueprints.put(blueprint.getName(), blueprint);
        }
        for (Blueprint savedBlueprint : savedBlueprints) {
            resultBlueprints.put(savedBlueprint.getName(), savedBlueprint);
        }
        return resultBlueprints.values();
    }

    private Set<String> collectNames(Set<Blueprint> failedToUpdate) {
        return failedToUpdate.stream().map(Blueprint::getName).collect(Collectors.toSet());
    }

    private Set<Blueprint> addMissingBlueprints(IdentityUser user, Set<Blueprint> blueprints) {
        Set<Blueprint> resultList = new HashSet<>();
        LOGGER.info("Adding default blueprints which are missing for the user.");
        for (String diffBlueprintName : collectDeviationOfExistingBlueprintsAndDefaultBlueprints(blueprints)) {
            Blueprint blueprintFromCache = defaultBlueprintCache.defaultBlueprints().get(diffBlueprintName);
            LOGGER.info("Default Blueprint '{}' needs to add for the '{}' user because the default blueprint missing.", diffBlueprintName, user.getUserId());
            resultList.add(prepateBlueprint(user, blueprintFromCache, blueprintFromCache));
        }
        LOGGER.info("Finished to add default blueprints which are missing for the user.");
        return resultList;
    }

    private Set<Blueprint> updateDefaultBlueprints(IdentityUser user, Set<Blueprint> blueprints) {
        Set<Blueprint> resultList = new HashSet<>();
        LOGGER.info("Updating default blueprints which are contains text modifications.");
        for (Blueprint blueprintFromDatabase : blueprints) {
            Blueprint newBlueprint = defaultBlueprintCache.defaultBlueprints().get(blueprintFromDatabase.getName());
            if (defaultBlueprintExistInTheCache(newBlueprint) && defaultBlueprintContainsNewTexts(blueprintFromDatabase, newBlueprint)) {
                LOGGER.info("Default Blueprint '{}' needs to modify for the '{}' user because the blueprint text changed.",
                        blueprintFromDatabase.getName(), user.getUserId());
                resultList.add(prepateBlueprint(user, blueprintFromDatabase, newBlueprint));
            }
        }
        LOGGER.info("Finished to Update default blueprints which are contains text modifications.");
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
        LOGGER.info("Collecting Blueprints which are missing from the defaults.");
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
        LOGGER.info("Finished to collect the default blueprints which are missing: {}.", diff);
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
