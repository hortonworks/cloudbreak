package cli

import (
	"strings"
	"time"

	"net/http"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/recipes"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"github.com/urfave/cli"
)

var recipeHeader []string = []string{"Name", "Description", "Execution Type"}

type recipeOut struct {
	Name          string `json:"Name" yaml:"Name"`
	Description   string `json:"Description" yaml:"Description"`
	ExecutionType string `json:"ExecutionType" yaml:"ExecutionType"`
}

func (r *recipeOut) DataAsStringArray() []string {
	return []string{r.Name, r.Description, r.ExecutionType}
}

func CreateRecipeFromUrl(c *cli.Context) {
	checkRequiredFlags(c)

	log.Infof("[CreateRecipeFromUrl] creating recipe from a URL")
	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	urlLocation := c.String(FlURL.Name)
	createRecipeImpl(
		cbClient.Cloudbreak.Recipes,
		c.String(FlName.Name),
		c.String(FlDescription.Name),
		c.Bool(FlPublic.Name),
		getExecutionType(c.String(FlExecutionType.Name)),
		utils.ReadContentFromURL(urlLocation, new(http.Client)))
}

func CreateRecipeFromFile(c *cli.Context) {
	checkRequiredFlags(c)

	log.Infof("[CreateRecipeFromFile] creating recipe from a file")
	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	fileLocation := c.String(FlFile.Name)
	createRecipeImpl(
		cbClient.Cloudbreak.Recipes,
		c.String(FlName.Name),
		c.String(FlDescription.Name),
		c.Bool(FlPublic.Name),
		getExecutionType(c.String(FlExecutionType.Name)),
		utils.ReadFile(fileLocation))
}

func getExecutionType(executionType string) string {
	switch strings.ToLower(executionType) {
	case "pre":
		return "PRE"
	case "post":
		return "POST"
	default:
		utils.LogErrorMessageAndExit("Recipe type not supported")
		panic(3)
	}
}

type recipeClient interface {
	PostPrivateRecipe(params *recipes.PostPrivateRecipeParams) (*recipes.PostPrivateRecipeOK, error)
	PostPublicRecipe(params *recipes.PostPublicRecipeParams) (*recipes.PostPublicRecipeOK, error)
}

func createRecipeImpl(client recipeClient, name string, description string, public bool, executionType string, recipeContent []byte) *models_cloudbreak.RecipeResponse {
	defer utils.TimeTrack(time.Now(), "create recipe")
	recipeRequest := &models_cloudbreak.RecipeRequest{
		Name:        name,
		Description: &description,
		Content:     string(recipeContent),
		RecipeType:  &executionType,
	}
	var recipe *models_cloudbreak.RecipeResponse
	if public {
		log.Infof("[createRecipeImpl] sending create public recipe request")
		resp, err := client.PostPublicRecipe(recipes.NewPostPublicRecipeParams().WithBody(recipeRequest))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		recipe = resp.Payload
	} else {
		log.Infof("[createRecipeImpl] sending create private recipe request")
		resp, err := client.PostPrivateRecipe(recipes.NewPostPrivateRecipeParams().WithBody(recipeRequest))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		recipe = resp.Payload
	}
	log.Infof("[createRecipeImpl] recipe created: %s (id: %d)", recipe.Name, recipe.ID)
	return recipe
}

func DescribeRecipe(c *cli.Context) {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "describe recipe")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	output := utils.Output{Format: c.String(FlOutput.Name)}
	resp, err := cbClient.Cloudbreak.Recipes.GetPublicRecipe(recipes.NewGetPublicRecipeParams().WithName(c.String(FlName.Name)))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	recipe := resp.Payload
	output.Write(recipeHeader, &recipeOut{recipe.Name, *recipe.Description, *recipe.RecipeType})
}

func DeleteRecipe(c *cli.Context) {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "delete recipe")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	name := c.String(FlName.Name)
	log.Infof("[DeleteRecipe] sending delete recipe request with name: %s", name)
	if err := cbClient.Cloudbreak.Recipes.DeletePublicRecipe(recipes.NewDeletePublicRecipeParams().WithName(name)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteRecipe] recipe deleted, name: %s", name)
}

func ListRecipes(c *cli.Context) {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "list recipes")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	output := utils.Output{Format: c.String(FlOutput.Name)}
	listRecipesImpl(cbClient.Cloudbreak.Recipes, output.WriteList)
}

type getPublicsRecipeClient interface {
	GetPublicsRecipe(*recipes.GetPublicsRecipeParams) (*recipes.GetPublicsRecipeOK, error)
}

func listRecipesImpl(client getPublicsRecipeClient, writer func([]string, []utils.Row)) {
	log.Infof("[listRecipesImpl] sending recipe list request")
	recipesResp, err := client.GetPublicsRecipe(recipes.NewGetPublicsRecipeParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	tableRows := []utils.Row{}
	for _, recipe := range recipesResp.Payload {
		tableRows = append(tableRows, &recipeOut{recipe.Name, *recipe.Description, *recipe.RecipeType})
	}

	writer(recipeHeader, tableRows)
}
