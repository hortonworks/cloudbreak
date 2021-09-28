package com.sequenceiq.cloudbreak.service.recipe;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.cluster.host.GeneratedRecipe;
import com.sequenceiq.cloudbreak.repository.GeneratedRecipeRepository;

@Service
public class GeneratedRecipeService {

    @Inject
    private GeneratedRecipeRepository generatedRecipeRepository;

    public GeneratedRecipe save(GeneratedRecipe generatedRecipe) {
        return generatedRecipeRepository.save(generatedRecipe);
    }

    public void deleteAll(Set<GeneratedRecipe> generatedRecipeSet) {
        generatedRecipeRepository.deleteAll(generatedRecipeSet);
    }

    public void saveAll(Set<GeneratedRecipe> generatedRecipeSet) {
        generatedRecipeRepository.saveAll(generatedRecipeSet);
    }
}
