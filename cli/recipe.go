package cli

import (
	"strings"
	"time"

	"net/http"

	"encoding/base64"
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v1recipes"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"github.com/urfave/cli"
)

var recipeHeader = []string{"Name", "Description", "Execution Type"}

type recipeOut struct {
	Name          string `json:"Name" yaml:"Name"`
	Description   string `json:"Description" yaml:"Description"`
	ExecutionType string `json:"ExecutionType" yaml:"ExecutionType"`
}

func (r *recipeOut) DataAsStringArray() []string {
	return []string{r.Name, r.Description, r.ExecutionType}
}

func CreateRecipeFromUrl(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)

	log.Infof("[CreateRecipeFromUrl] creating recipe from a URL")
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	urlLocation := c.String(FlURL.Name)
	createRecipeImpl(
		cbClient.Cloudbreak.V1recipes,
		c.String(FlName.Name),
		c.String(FlDescriptionOptional.Name),
		c.Bool(FlPublicOptional.Name),
		getExecutionType(c.String(FlExecutionType.Name)),
		base64.StdEncoding.EncodeToString(utils.ReadContentFromURL(urlLocation, new(http.Client))))
}

func CreateRecipeFromFile(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)

	log.Infof("[CreateRecipeFromFile] creating recipe from a file")
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	fileLocation := c.String(FlFile.Name)
	createRecipeImpl(
		cbClient.Cloudbreak.V1recipes,
		c.String(FlName.Name),
		c.String(FlDescriptionOptional.Name),
		c.Bool(FlPublicOptional.Name),
		getExecutionType(c.String(FlExecutionType.Name)),
		base64.StdEncoding.EncodeToString(utils.ReadFile(fileLocation)))
}

func getExecutionType(executionType string) string {
	switch strings.ToLower(executionType) {
	case "pre-ambari-start":
		return "PRE_AMBARI_START"
	case "post-ambari-start":
		return "POST_AMBARI_START"
	case "post-cluster-install":
		return "POST_CLUSTER_INSTALL"
	case "pre-termination":
		return "PRE_TERMINATION"
	default:
		utils.LogErrorMessageAndExit("Recipe type not supported")
		panic(3)
	}
}

type recipeClient interface {
	PostPrivateRecipe(params *v1recipes.PostPrivateRecipeParams) (*v1recipes.PostPrivateRecipeOK, error)
	PostPublicRecipe(params *v1recipes.PostPublicRecipeParams) (*v1recipes.PostPublicRecipeOK, error)
}

func createRecipeImpl(client recipeClient, name string, description string, public bool, executionType string, recipeContent string) *models_cloudbreak.RecipeResponse {
	defer utils.TimeTrack(time.Now(), "create recipe")
	recipeRequest := &models_cloudbreak.RecipeRequest{
		Name:        name,
		Description: &description,
		Content:     recipeContent,
		RecipeType:  &executionType,
	}
	var recipe *models_cloudbreak.RecipeResponse
	if public {
		log.Infof("[createRecipeImpl] sending create public recipe request")
		resp, err := client.PostPublicRecipe(v1recipes.NewPostPublicRecipeParams().WithBody(recipeRequest))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		recipe = resp.Payload
	} else {
		log.Infof("[createRecipeImpl] sending create private recipe request")
		resp, err := client.PostPrivateRecipe(v1recipes.NewPostPrivateRecipeParams().WithBody(recipeRequest))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		recipe = resp.Payload
	}
	log.Infof("[createRecipeImpl] recipe created: %s (id: %d)", recipe.Name, recipe.ID)
	return recipe
}

func DescribeRecipe(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "describe recipe")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	resp, err := cbClient.Cloudbreak.V1recipes.GetPublicRecipe(v1recipes.NewGetPublicRecipeParams().WithName(c.String(FlName.Name)))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	recipe := resp.Payload
	output.Write(recipeHeader, &recipeOut{recipe.Name, *recipe.Description, *recipe.RecipeType})
}

func DeleteRecipe(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "delete recipe")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	name := c.String(FlName.Name)
	log.Infof("[DeleteRecipe] sending delete recipe request with name: %s", name)
	if err := cbClient.Cloudbreak.V1recipes.DeletePublicRecipe(v1recipes.NewDeletePublicRecipeParams().WithName(name)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteRecipe] recipe deleted, name: %s", name)
}

func ListRecipes(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list recipes")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	listRecipesImpl(cbClient.Cloudbreak.V1recipes, output.WriteList)
}

type getPublicsRecipeClient interface {
	GetPublicsRecipe(*v1recipes.GetPublicsRecipeParams) (*v1recipes.GetPublicsRecipeOK, error)
}

func listRecipesImpl(client getPublicsRecipeClient, writer func([]string, []utils.Row)) {
	log.Infof("[listRecipesImpl] sending recipe list request")
	recipesResp, err := client.GetPublicsRecipe(v1recipes.NewGetPublicsRecipeParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	tableRows := []utils.Row{}
	for _, recipe := range recipesResp.Payload {
		tableRows = append(tableRows, &recipeOut{recipe.Name, *recipe.Description, *recipe.RecipeType})
	}

	writer(recipeHeader, tableRows)
}
