package recipe

import (
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"strconv"
	"strings"
	"time"

	"encoding/base64"

	log "github.com/Sirupsen/logrus"
	v4recipe "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_recipes"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/dp-cli-common/utils"
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
	ID      string `json:"ID" yaml:"ID"`
}

type recipeOutTableDescribe struct {
	*recipeOut
	ID string `json:"ID" yaml:"ID"`
}

func (r *recipeOut) DataAsStringArray() []string {
	return []string{r.Name, r.Description, r.ExecutionType}
}

func (r *recipeOutJsonDescribe) DataAsStringArray() []string {
	return append(r.recipeOut.DataAsStringArray(), r.ID)
}

func (r *recipeOutTableDescribe) DataAsStringArray() []string {
	return append(r.recipeOut.DataAsStringArray(), r.ID)
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

	log.Infof("[createRecipeImpl] recipe created: %s (id: %d)", recipe.Name, recipe.ID)
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
		output.Write(append(recipeHeader, "ContentAsBase64", "ID"), &recipeOutJsonDescribe{&recipeOut{recipe.Name, *recipe.Description, *recipe.Type}, recipe.Content, strconv.FormatInt(recipe.ID, 10)})
	} else {
		output.Write(append(recipeHeader, "ID"), &recipeOutTableDescribe{&recipeOut{recipe.Name, *recipe.Description, *recipe.Type}, strconv.FormatInt(recipe.ID, 10)})
	}
}

func DeleteRecipe(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "delete recipe")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	name := c.String(fl.FlName.Name)
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	log.Infof("[DeleteRecipe] sending delete recipe request with name: %s", name)
	if _, err := cbClient.Cloudbreak.V4WorkspaceIDRecipes.DeleteRecipeInWorkspace(v4recipe.NewDeleteRecipeInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(name)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteRecipe] recipe deleted, name: %s", name)
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
