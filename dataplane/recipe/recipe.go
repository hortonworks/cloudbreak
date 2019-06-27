package recipe

import (
	"strings"
	"time"

	"github.com/hortonworks/cb-cli/dataplane/oauth"

	"encoding/base64"

	v4recipe "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_recipes"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/dp-cli-common/utils"
	log "github.com/sirupsen/logrus"
	"github.com/urfave/cli"
)

var recipeHeader = []string{"Name", "Description", "Execution Type"}

type recipeOut struct {
	Name          string `json:"Name" yaml:"Name"`
	Description   string `json:"Description" yaml:"Description"`
	ExecutionType string `json:"ExecutionType" yaml:"ExecutionType"`
}

type recipeOutJsonDescribe struct {
	*recipeOut
	Content string `json:"RecipeTextAsBase64" yaml:"RecipeTextAsBase64"`
	Crn     string `json:"CRN" yaml:"CRN"`
}

type recipeOutTableDescribe struct {
	*recipeOut
	Crn string `json:"CRN" yaml:"CRN"`
}

func (r *recipeOut) DataAsStringArray() []string {
	return []string{r.Name, r.Description, r.ExecutionType}
}

func (r *recipeOutJsonDescribe) DataAsStringArray() []string {
	return append(r.recipeOut.DataAsStringArray(), r.Crn)
}

func (r *recipeOutTableDescribe) DataAsStringArray() []string {
	return append(r.recipeOut.DataAsStringArray(), r.Crn)
}

func CreateRecipeFromFile(c *cli.Context) {

	log.Infof("[CreateRecipeFromFile] creating recipe from a file")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	fileLocation := c.String(fl.FlFile.Name)
	createRecipeImpl(
		cbClient.Cloudbreak.V4WorkspaceIDRecipes,
		c.Int64(fl.FlWorkspaceOptional.Name),
		c.String(fl.FlName.Name),
		c.String(fl.FlDescriptionOptional.Name),
		getExecutionType(c.String(fl.FlExecutionType.Name)),
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
	CreateRecipeInWorkspace(params *v4recipe.CreateRecipeInWorkspaceParams) (*v4recipe.CreateRecipeInWorkspaceOK, error)
}

func createRecipeImpl(client recipeClient, workspaceID int64, name string, description string, executionType string, recipeContent string) *model.RecipeV4Response {
	defer utils.TimeTrack(time.Now(), "create recipe")
	recipeRequest := &model.RecipeV4Request{
		Name:        name,
		Description: &description,
		Content:     recipeContent,
		Type:        &executionType,
	}
	var recipe *model.RecipeV4Response
	log.Infof("[createRecipeImpl] sending create public recipe request")
	resp, err := client.CreateRecipeInWorkspace(v4recipe.NewCreateRecipeInWorkspaceParams().WithWorkspaceID(workspaceID).WithBody(recipeRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	recipe = resp.Payload

	log.Infof("[createRecipeImpl] recipe created: %s (crn: %s)", recipe.Name, recipe.Crn)
	return recipe
}

func DescribeRecipe(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "describe recipe")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	resp, err := cbClient.Cloudbreak.V4WorkspaceIDRecipes.GetRecipeInWorkspace(v4recipe.NewGetRecipeInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(c.String(fl.FlName.Name)))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	recipe := resp.Payload
	if output.Format != "table" {
		output.Write(append(recipeHeader, "ContentAsBase64", "CRN"), &recipeOutJsonDescribe{&recipeOut{recipe.Name, *recipe.Description, *recipe.Type}, recipe.Content, recipe.Crn})
	} else {
		output.Write(append(recipeHeader, "CRN"), &recipeOutTableDescribe{&recipeOut{recipe.Name, *recipe.Description, *recipe.Type}, recipe.Crn})
	}
}

func DeleteRecipes(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "delete recipes")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	names := c.StringSlice(fl.FlNames.Name)
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	log.Infof("[DeleteRecipes] sending delete recipe request with names: %s", names)
	if _, err := cbClient.Cloudbreak.V4WorkspaceIDRecipes.DeleteRecipesInWorkspace(v4recipe.NewDeleteRecipesInWorkspaceParams().WithWorkspaceID(workspaceID).WithBody(names)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteRecipes] recipes deleted, names: %s", names)
}

func ListRecipes(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "list recipes")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	listRecipesImpl(cbClient.Cloudbreak.V4WorkspaceIDRecipes, output.WriteList, workspaceID)
}

type getPublicsRecipeClient interface {
	ListRecipesByWorkspace(*v4recipe.ListRecipesByWorkspaceParams) (*v4recipe.ListRecipesByWorkspaceOK, error)
}

func listRecipesImpl(client getPublicsRecipeClient, writer func([]string, []utils.Row), workspaceID int64) {
	log.Infof("[listRecipesImpl] sending recipe list request")
	recipesResp, err := client.ListRecipesByWorkspace(v4recipe.NewListRecipesByWorkspaceParams().WithWorkspaceID(workspaceID))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, recipe := range recipesResp.Payload.Responses {
		tableRows = append(tableRows, &recipeOut{*recipe.Name, utils.SafeStringConvert(recipe.Description), *recipe.Type})
	}

	writer(recipeHeader, tableRows)
}
