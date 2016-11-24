package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/recipes"
	"github.com/hortonworks/hdc-cli/models"
	"strconv"
	"strings"
	"sync"
	"time"
)

func (c *Cloudbreak) CreateRecipe(skeleton ClusterSkeleton, masterRecipes chan int64, workerRecipes chan int64, wg *sync.WaitGroup) {
	defer wg.Done()
	createRecipeImpl(skeleton, masterRecipes, workerRecipes, c.Cloudbreak.Recipes.PostRecipesAccount)
}

func createRecipeImpl(skeleton ClusterSkeleton, masterRecipes chan int64, workerRecipes chan int64,
	postPublicRecipe func(params *recipes.PostRecipesAccountParams) (*recipes.PostRecipesAccountOK, error)) {

	defer timeTrack(time.Now(), "create recipe")

	var wgr sync.WaitGroup
	wgr.Add(2)

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

	wgr.Wait()
}

func createRecipe(recipe Recipe, postPublicRecipe func(params *recipes.PostRecipesAccountParams) (*recipes.PostRecipesAccountOK, error)) int64 {
	recipeName := getRecipeName(recipe.URI)
	recipeRequest := models.RecipeRequest{
		Name: recipeName,
	}
	if recipe.Phase == PRE {
		recipeRequest.PreURL = &recipe.URI
	} else {
		recipeRequest.PostURL = &recipe.URI
	}

	log.Infof("[createRecipe] creating recipe with name: %s", recipeName)
	resp, err := postPublicRecipe(&recipes.PostRecipesAccountParams{Body: &recipeRequest})

	if err != nil {
		logErrorAndExit(createRecipeImpl, err.Error())
	}

	log.Infof("[createRecipe] recipe created, name: %s, id: %d", resp.Payload.Name, resp.Payload.ID)
	return *resp.Payload.ID
}

func getRecipeName(url string) string {
	name := "hrec" + strconv.FormatInt(time.Now().UnixNano(), 10)
	perIndex := strings.LastIndex(url, "/")
	if -1 != perIndex {
		if len(url) > perIndex+1 {
			urlName := url[perIndex+1:]
			if strings.Contains(urlName, ".") {
				urlName = strings.Replace(urlName, ".", "", -1)
			}
			if strings.Contains(urlName, "?") {
				urlName = urlName[0:strings.LastIndex(urlName, "?")]
			}
			if len(urlName) > 0 {
				name += urlName
			}
		}
	}
	return name
}

func (c *Cloudbreak) GetPublicRecipes() []*models.RecipeResponse {
	defer timeTrack(time.Now(), "get public recipes")
	resp, err := c.Cloudbreak.Recipes.GetRecipesAccount(&recipes.GetRecipesAccountParams{})
	if err != nil {
		logErrorAndExit(c.GetPublicRecipes, err.Error())
	}
	return resp.Payload
}

func (c *Cloudbreak) DeleteRecipe(name string) error {
	defer timeTrack(time.Now(), "delete recipe")
	log.Infof("[DeleteRecipe] delete recipe: %s", name)
	return c.Cloudbreak.Recipes.DeleteRecipesAccountName(&recipes.DeleteRecipesAccountNameParams{Name: name})
}
