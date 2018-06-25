package com.sequenceiq.cloudbreak.init.blueprint;

import static com.sequenceiq.cloudbreak.api.model.ResourceStatus.DEFAULT;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
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

    public boolean addingDefaultBlueprintsAreNecessaryForTheUser(Collection<Blueprint> blueprints) {
        Map<String, Blueprint> defaultBlueprints = defaultBlueprintCache.defaultBlueprints();
        for (Blueprint blueprintFromDatabase : blueprints) {
            Blueprint defaultBlueprint = defaultBlueprints.get(blueprintFromDatabase.getName());
            if (mustUpdateTheExistingBlueprint(blueprintFromDatabase, defaultBlueprint)) {
                return true;
            }
        }
        if (isNewUserOrDeletedEveryDefaultBlueprint(blueprints)) {
            return true;
        }
        return defaultBlueprintDoesNotExistInTheDatabase(blueprints);
    }

    public Set<Blueprint> loadBlueprintsForTheSpecifiedUser(IdentityUser user, Set<Blueprint> blueprints) {
        Set<Blueprint> blueprintsWhichShouldBeUpdate = updateDefaultBlueprints(user, blueprints);
        Set<Blueprint> blueprintsWhichAreMissing = addMissingBlueprints(user, blueprints);
        try {
            LOGGER.info("Prepare Blueprint set for the user '{}' after the modifications.", user.getUserId());
            blueprintsWhichAreMissing.addAll(blueprintsWhichShouldBeUpdate);
            if (!blueprintsWhichAreMissing.isEmpty()) {
                return Sets.newHashSet(getResultSetFromUpdateAndOriginalBlueprints(blueprints, blueprintsWhichAreMissing));
            }
        } catch (Exception e) {
            LOGGER.error("Blueprints {} is not available for {} user.", collectNames(blueprintsWhichAreMissing), user.getUsername());
        }
        return blueprints;
    }

    private Iterable<Blueprint> getResultSetFromUpdateAndOriginalBlueprints(Iterable<Blueprint> blueprints, Iterable<Blueprint> blueprintsWhichAreMissing) {
        LOGGER.info("Updating blueprints which should be modified.");
        Iterable<Blueprint> savedBlueprints = blueprintRepository.saveAll(blueprintsWhichAreMissing);
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

    private Set<String> collectNames(Collection<Blueprint> failedToUpdate) {
        return failedToUpdate.stream().map(Blueprint::getName).collect(Collectors.toSet());
    }

    private Set<Blueprint> addMissingBlueprints(IdentityUser user, Iterable<Blueprint> blueprints) {
        Set<Blueprint> resultList = new HashSet<>();
        LOGGER.info("Adding default blueprints which are missing for the user.");
        for (Map.Entry<String, Blueprint> diffBlueprint : collectDeviationOfExistingBlueprintsAndDefaultBlueprints(blueprints).entrySet()) {
            LOGGER.info("Default Blueprint '{}' needs to add for the '{}' user because the default validation missing.",
                    diffBlueprint.getKey(), user.getUserId());
            resultList.add(setupBlueprint(user, diffBlueprint.getValue()));
        }
        LOGGER.info("Finished to add default blueprints which are missing for the user.");
        return resultList;
    }

    private Set<Blueprint> updateDefaultBlueprints(IdentityUser user, Iterable<Blueprint> blueprints) {
        Set<Blueprint> resultList = new HashSet<>();
        LOGGER.info("Updating default blueprints which are contains text modifications.");
        Map<String, Blueprint> defaultBlueprints = defaultBlueprintCache.defaultBlueprints();
        for (Blueprint blueprintFromDatabase : blueprints) {
            Blueprint newBlueprint = defaultBlueprints.get(blueprintFromDatabase.getName());
            if (defaultBlueprintExistInTheCache(newBlueprint)
                    && (defaultBlueprintContainsNewTexts(blueprintFromDatabase, newBlueprint)
                    || defaultBlueprintContainsNewDescription(blueprintFromDatabase, newBlueprint))) {
                LOGGER.info("Default Blueprint '{}' needs to modify for the '{}' user because the validation text changed.",
                        blueprintFromDatabase.getName(), user.getUserId());
                resultList.add(prepateBlueprint(user, blueprintFromDatabase, newBlueprint));
            }
        }
        LOGGER.info("Finished to Update default blueprints which are contains text modifications.");
        return resultList;
    }

    private Blueprint prepateBlueprint(IdentityUser user, Blueprint blueprintFromDatabase, Blueprint newBlueprint) {
        setupBlueprint(user, blueprintFromDatabase);
        blueprintFromDatabase.setBlueprintText(newBlueprint.getBlueprintText());
        blueprintFromDatabase.setDescription(newBlueprint.getDescription());
        blueprintFromDatabase.setHostGroupCount(newBlueprint.getHostGroupCount());
        blueprintFromDatabase.setInputParameters(newBlueprint.getInputParameters());
        blueprintFromDatabase.setAmbariName(newBlueprint.getAmbariName());
        return blueprintFromDatabase;
    }

    private Blueprint setupBlueprint(IdentityUser user, Blueprint blueprint) {
        blueprint.setAccount(user.getAccount());
        blueprint.setOwner(user.getUserId());
        blueprint.setPublicInAccount(true);
        blueprint.setStatus(DEFAULT);
        return blueprint;
    }

    private Map<String, Blueprint> collectDeviationOfExistingBlueprintsAndDefaultBlueprints(Iterable<Blueprint> blueprints) {
        LOGGER.info("Collecting Blueprints which are missing from the defaults.");
        Map<String, Blueprint> diff = new HashMap<>();
        for (Entry<String, Blueprint> stringBlueprintEntry : defaultBlueprintCache.defaultBlueprints().entrySet()) {
            boolean contains = false;
            for (Blueprint blueprint : blueprints) {
                if (isRegisteregBlueprintAndDefaultBlueprintIsTheSame(stringBlueprintEntry, blueprint)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                diff.put(stringBlueprintEntry.getKey(), stringBlueprintEntry.getValue());
            }
        }
        LOGGER.info("Finished to collect the default blueprints which are missing: {}.", diff);
        return diff;
    }

    private boolean isRegisteregBlueprintAndDefaultBlueprintIsTheSame(Entry<String, Blueprint> stringBlueprintEntry, Blueprint blueprint) {
        return blueprint.getName().equals(stringBlueprintEntry.getKey()) && isDefaultBlueprint(blueprint);
    }

    private boolean isDefaultBlueprint(Blueprint bp) {
        return DEFAULT.equals(bp.getStatus());
    }

    private boolean defaultBlueprintContainsNewTexts(Blueprint bp, Blueprint blueprint) {
        return !bp.getBlueprintText().equals(blueprint.getBlueprintText());
    }

    private boolean defaultBlueprintContainsNewDescription(Blueprint bp, Blueprint blueprint) {
        return !bp.getDescription().equals(blueprint.getDescription());
    }

    private boolean isNewUserOrDeletedEveryDefaultBlueprint(Collection<Blueprint> blueprints) {
        return blueprints.isEmpty();
    }

    private boolean mustUpdateTheExistingBlueprint(Blueprint blueprintFromDatabase, Blueprint defaultBlueprint) {
        return isDefaultBlueprint(blueprintFromDatabase)
                && defaultBlueprintExistInTheCache(defaultBlueprint)
                && (defaultBlueprintContainsNewTexts(blueprintFromDatabase, defaultBlueprint)
                || defaultBlueprintContainsNewDescription(blueprintFromDatabase, defaultBlueprint));
    }

    private boolean defaultBlueprintDoesNotExistInTheDatabase(Iterable<Blueprint> blueprints) {
        return !collectDeviationOfExistingBlueprintsAndDefaultBlueprints(blueprints).isEmpty();
    }

    private boolean defaultBlueprintExistInTheCache(Blueprint actualDefaultBlueprint) {
        return actualDefaultBlueprint != null;
    }
}
