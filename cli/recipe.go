package cli

import (
	"strconv"
	"strings"
	"time"

	"net/http"

	"encoding/base64"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v3_organization_id_recipes"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"github.com/urfave/cli"
)

var recipeHeader = []string{"Name", "Description", "Execution Type"}

type recipeOut struct {
	Name          string `json:"Name" yaml:"Name"`
	Description   string `json:"Description" yaml:"Description"`
	ExecutionType string `json:"ExecutionType" yaml:"ExecutionType"`
}

type recipeOutDescribe struct {
	*recipeOut
	ID string `json:"ID" yaml:"ID"`
}

func (r *recipeOut) DataAsStringArray() []string {
	return []string{r.Name, r.Description, r.ExecutionType}
}

func (r *recipeOutDescribe) DataAsStringArray() []string {
	return append(r.recipeOut.DataAsStringArray(), r.ID)
}

func CreateRecipeFromUrl(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)

	log.Infof("[CreateRecipeFromUrl] creating recipe from a URL")
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	urlLocation := c.String(FlURL.Name)
	createRecipeImpl(
		cbClient.Cloudbreak.V3OrganizationIDRecipes,
		c.Int64(FlOrganizationOptional.Name),
		c.String(FlName.Name),
		c.String(FlDescriptionOptional.Name),
		getExecutionType(c.String(FlExecutionType.Name)),
		base64.StdEncoding.EncodeToString(utils.ReadContentFromURL(urlLocation, new(http.Client))))
}

func CreateRecipeFromFile(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)

	log.Infof("[CreateRecipeFromFile] creating recipe from a file")
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	fileLocation := c.String(FlFile.Name)
	createRecipeImpl(
		cbClient.Cloudbreak.V3OrganizationIDRecipes,
		c.Int64(FlOrganizationOptional.Name),
		c.String(FlName.Name),
		c.String(FlDescriptionOptional.Name),
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
	CreateRecipeInOrganization(params *v3_organization_id_recipes.CreateRecipeInOrganizationParams) (*v3_organization_id_recipes.CreateRecipeInOrganizationOK, error)
}

func createRecipeImpl(client recipeClient, orgID int64, name string, description string, executionType string, recipeContent string) *models_cloudbreak.RecipeResponse {
	defer utils.TimeTrack(time.Now(), "create recipe")
	recipeRequest := &models_cloudbreak.RecipeRequest{
		Name:        name,
		Description: &description,
		Content:     recipeContent,
		RecipeType:  &executionType,
	}
	var recipe *models_cloudbreak.RecipeResponse
	log.Infof("[createRecipeImpl] sending create public recipe request")
	resp, err := client.CreateRecipeInOrganization(v3_organization_id_recipes.NewCreateRecipeInOrganizationParams().WithOrganizationID(orgID).WithBody(recipeRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	recipe = resp.Payload

	log.Infof("[createRecipeImpl] recipe created: %s (id: %d)", recipe.Name, recipe.ID)
	return recipe
}

func DescribeRecipe(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "describe recipe")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	orgID := c.Int64(FlOrganizationOptional.Name)
	resp, err := cbClient.Cloudbreak.V3OrganizationIDRecipes.GetRecipeInOrganization(v3_organization_id_recipes.NewGetRecipeInOrganizationParams().WithOrganizationID(orgID).WithName(c.String(FlName.Name)))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	recipe := resp.Payload
	output.Write(append(recipeHeader, "ID"), &recipeOutDescribe{&recipeOut{recipe.Name, *recipe.Description, *recipe.RecipeType}, strconv.FormatInt(recipe.ID, 10)})
}

func DeleteRecipe(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "delete recipe")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	name := c.String(FlName.Name)
	orgID := c.Int64(FlOrganizationOptional.Name)
	log.Infof("[DeleteRecipe] sending delete recipe request with name: %s", name)
	if _, err := cbClient.Cloudbreak.V3OrganizationIDRecipes.DeleteRecipeInOrganization(v3_organization_id_recipes.NewDeleteRecipeInOrganizationParams().WithOrganizationID(orgID).WithName(name)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteRecipe] recipe deleted, name: %s", name)
}

func ListRecipes(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list recipes")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	orgID := c.Int64(FlOrganizationOptional.Name)
	listRecipesImpl(cbClient.Cloudbreak.V3OrganizationIDRecipes, output.WriteList, orgID)
}

type getPublicsRecipeClient interface {
	ListRecipesByOrganization(*v3_organization_id_recipes.ListRecipesByOrganizationParams) (*v3_organization_id_recipes.ListRecipesByOrganizationOK, error)
}

func listRecipesImpl(client getPublicsRecipeClient, writer func([]string, []utils.Row), orgID int64) {
	log.Infof("[listRecipesImpl] sending recipe list request")
	recipesResp, err := client.ListRecipesByOrganization(v3_organization_id_recipes.NewListRecipesByOrganizationParams().WithOrganizationID(orgID))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	tableRows := []utils.Row{}
	for _, recipe := range recipesResp.Payload {
		tableRows = append(tableRows, &recipeOut{recipe.Name, *recipe.Description, *recipe.RecipeType})
	}

	writer(recipeHeader, tableRows)
}
