package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client_cloudbreak/recipes"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
	"strings"
	"sync"
	"time"
)

func (c *Cloudbreak) CreateRecipe(skeleton ClusterSkeleton, masterRecipes chan int64, workerRecipes chan int64, computeRecipes chan int64, wg *sync.WaitGroup) {
	defer wg.Done()
	createRecipeImpl(skeleton, masterRecipes, workerRecipes, computeRecipes, c.Cloudbreak.Recipes.PostRecipesAccount)
}

func createRecipeImpl(skeleton ClusterSkeleton, masterRecipes chan int64, workerRecipes chan int64, computeRecipes chan int64,
	postPublicRecipe func(params *recipes.PostRecipesAccountParams) (*recipes.PostRecipesAccountOK, error)) {

	defer timeTrack(time.Now(), "create recipe")

	var wgr sync.WaitGroup
	wgr.Add(3)

	go func() {
		defer wgr.Done()
		for _, r := range skeleton.Master.Recipes {
			id := createRecipe(r, postPublicRecipe)
			masterRecipes <- id
		}
		close(masterRecipes)
	}()

	go func() {
		defer wgr.Done()
		for _, r := range skeleton.Worker.Recipes {
			id := createRecipe(r, postPublicRecipe)
			workerRecipes <- id
		}
		close(workerRecipes)
	}()

	go func() {
		defer wgr.Done()
		for _, r := range skeleton.Compute.Recipes {
			id := createRecipe(r, postPublicRecipe)
			computeRecipes <- id
		}
		close(computeRecipes)
	}()

	wgr.Wait()
}

func createRecipe(recipe Recipe, postPublicRecipe func(params *recipes.PostRecipesAccountParams) (*recipes.PostRecipesAccountOK, error)) int64 {
	recipeRequest := createRecipeRequest(recipe)

	log.Infof("[createRecipe] creating recipe with URI: %s", recipe.URI)
	resp, err := postPublicRecipe(&recipes.PostRecipesAccountParams{Body: recipeRequest})

	if err != nil {
		logErrorAndExit(err)
	}

	log.Infof("[createRecipe] recipe created, name: %s, uri: %s, id: %d", resp.Payload.Name, recipe.URI, *resp.Payload.ID)
	return *resp.Payload.ID
}

func createRecipeRequests(recipes []Recipe) []*models_cloudbreak.RecipeRequest {
	var requests = make([]*models_cloudbreak.RecipeRequest, 0)

	for _, r := range recipes {
		requests = append(requests, createRecipeRequest(r))
	}

	return requests
}

func createRecipeRequest(recipe Recipe) *models_cloudbreak.RecipeRequest {
	recipeRequest := models_cloudbreak.RecipeRequest{
		URI:        &recipe.URI,
		RecipeType: strings.ToUpper(recipe.Phase),
	}

	return &recipeRequest
}

func (c *Cloudbreak) GetPublicRecipes() []*models_cloudbreak.RecipeResponse {
	defer timeTrack(time.Now(), "get public recipes")
	resp, err := c.Cloudbreak.Recipes.GetRecipesAccount(&recipes.GetRecipesAccountParams{})
	if err != nil {
		logErrorAndExit(err)
	}
	return resp.Payload
}

func (c *Cloudbreak) DeleteRecipe(name string) error {
	defer timeTrack(time.Now(), "delete recipe")
	log.Infof("[DeleteRecipe] delete recipe: %s", name)
	return c.Cloudbreak.Recipes.DeleteRecipesAccountName(&recipes.DeleteRecipesAccountNameParams{Name: name})
}
