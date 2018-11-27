package maintenancemode

import (
	"encoding/json"
	"fmt"
	"github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/dataplane/api/client/v3_workspace_id_stacks"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/cb-cli/dataplane/types"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

type maintenanceModeClient interface {
	SetClusterMaintenanceMode(params *v3_workspace_id_stacks.SetClusterMaintenanceModeParams) error
	PutClusterV3(params *v3_workspace_id_stacks.PutClusterV3Params) error
}

func EnableMaintenanceMode(c *cli.Context) {
	logrus.Infof("[EnableMaintenanceMode] enable maintenance mode")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	workspaceID, name := resolveWorkspaceIDAndStackName(c.Int64, c.String)
	toggleMaintenanceMode(cbClient.Cloudbreak.V3WorkspaceIDStacks, workspaceID, name, model.MaintenanceModeJSONStatusENABLED)
}

func resolveWorkspaceIDAndStackName(int64Finder func(string) int64, stringFinder func(string) string) (int64, string) {
	return int64Finder(flags.FlWorkspaceOptional.Name), stringFinder(flags.FlName.Name)
}

func toggleMaintenanceMode(client maintenanceModeClient, workspaceId int64, name string, status string) {
	request := &model.MaintenanceModeJSON{Status: status}
	err := client.SetClusterMaintenanceMode(v3_workspace_id_stacks.NewSetClusterMaintenanceModeParams().WithWorkspaceID(workspaceId).WithName(name).WithBody(request))
	if err == nil {
		logrus.Infof("[toggleMaintenanceMode] maintenance mode successfully set to: %s", status)
	} else {
		utils.LogErrorAndExit(err)
	}
}

func DisableMaintenanceMode(c *cli.Context) {
	logrus.Infof("[DisableMaintenanceMode] disable maintenance mode")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	workspaceID, name := resolveWorkspaceIDAndStackName(c.Int64, c.String)
	toggleMaintenanceMode(cbClient.Cloudbreak.V3WorkspaceIDStacks, workspaceID, name, model.MaintenanceModeJSONStatusDISABLED)
}

func ValidateRepositoryConfigurations(c *cli.Context) {
	logrus.Infof("[ValidateRepositoryConfigurations] validate repository configurations")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	workspaceID, name := resolveWorkspaceIDAndStackName(c.Int64, c.String)
	toggleMaintenanceMode(cbClient.Cloudbreak.V3WorkspaceIDStacks, workspaceID, name, model.MaintenanceModeJSONStatusVALIDATIONREQUESTED)
}

func ChangeHdpRepo(c *cli.Context) {
	logrus.Infof("[ChangeHdpRepo] change HDP repo")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	workspaceID, name := resolveWorkspaceIDAndStackName(c.Int64, c.String)

	stackDetails := &model.AmbariStackDetails{
		Version:                  c.String(flags.FlVersion.Name),
		VersionDefinitionFileURL: c.String(flags.FlVdfUrl.Name),
	}

	unmarshallCliInput(c.String, stackDetails)
	stackDetails.Stack = "HDP"
	updateStackRepoDetails(cbClient.Cloudbreak.V3WorkspaceIDStacks, workspaceID, name, stackDetails)
}

func unmarshallCliInput(stringFinder func(string) string, stackDetails *model.AmbariStackDetails) {
	jsonPath := stringFinder(flags.FlInputJson.Name)
	if len(jsonPath) != 0 {
		repoConfigPath := utils.ReadFile(jsonPath)
		if err := json.Unmarshal(repoConfigPath, &stackDetails); err != nil {
			utils.LogErrorAndExit(err)
		}
	}
}

func ChangeHdfRepo(c *cli.Context) {
	logrus.Infof("[ChangeHdfRepo] change HDF repo")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	workspaceID, name := resolveWorkspaceIDAndStackName(c.Int64, c.String)

	stackDetails := &model.AmbariStackDetails{
		Version:                  c.String(flags.FlVersion.Name),
		VersionDefinitionFileURL: c.String(flags.FlVdfUrl.Name),
		MpackURL:                 c.String(flags.FlMPackUrl.Name),
	}

	unmarshallCliInput(c.String, stackDetails)
	stackDetails.Stack = "HDF"
	updateStackRepoDetails(cbClient.Cloudbreak.V3WorkspaceIDStacks, workspaceID, name, stackDetails)

}
func ChangeAmbariRepo(c *cli.Context) {
	logrus.Infof("[ChangeAmbariRepo] change Ambari repo")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	workspaceID, name := resolveWorkspaceIDAndStackName(c.Int64, c.String)

	stackDetails := &model.AmbariStackDetails{
		Version:      c.String(flags.FlVersion.Name),
		StackBaseURL: c.String(flags.FlRepoUrl.Name),
		GpgKeyURL:    c.String(flags.FlRepoGpgUrl.Name),
	}

	unmarshallCliInput(c.String, stackDetails)
	stackDetails.Stack = "AMBARI"
	updateStackRepoDetails(cbClient.Cloudbreak.V3WorkspaceIDStacks, workspaceID, name, stackDetails)
}

func updateStackRepoDetails(client maintenanceModeClient, workspaceID int64, name string, stackDetails *model.AmbariStackDetails) {
	request := &model.UpdateCluster{
		AmbariStackDetails: stackDetails,
	}
	err := client.PutClusterV3(v3_workspace_id_stacks.NewPutClusterV3Params().WithWorkspaceID(workspaceID).WithName(name).WithBody(request))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
}

func GenerateHdpRepoJson(c *cli.Context) {
	stackDetails := &model.AmbariStackDetails{
		StackRepoID:              "____",
		OsType:                   "____",
		StackBaseURL:             "____",
		RepositoryVersion:        "____",
		VersionDefinitionFileURL: "____",
		UtilsRepoID:              "____",
		UtilsBaseURL:             "____",
		EnableGplRepo:            &(&types.B{B: true}).B,
		Mpacks:                   []*model.ManagementPackDetails{},
		Version:                  "____",
	}

	jsonBytes, err := json.MarshalIndent(stackDetails, "", "\t")
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	fmt.Printf("%s\n", string(jsonBytes))
}

func GenerateHdfRepoJson(c *cli.Context) {
	stackDetails := &model.AmbariStackDetails{
		StackRepoID:              "____",
		OsType:                   "____",
		StackBaseURL:             "____",
		RepositoryVersion:        "____",
		VersionDefinitionFileURL: "____",
		UtilsRepoID:              "____",
		UtilsBaseURL:             "____",
		EnableGplRepo:            &(&types.B{B: true}).B,
		MpackURL:                 "____",
		Mpacks: []*model.ManagementPackDetails{
			{
				Name:         &(&types.S{S: "____"}).S,
				PreInstalled: &(&types.B{B: false}).B,
			},
		},
		Version: "____",
	}

	jsonBytes, err := json.MarshalIndent(stackDetails, "", "\t")
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	fmt.Printf("%s\n", string(jsonBytes))
}

func GenerateAmbariRepoJson(c *cli.Context) {
	type AmbariRepoConfig struct {
		Version string `json:"version,omitempty"`

		StackBaseURL string `json:"stackBaseURL,omitempty"`

		GpgKeyURL string `json:"gpgKeyUrl,omitempty"`

		UtilsBaseURL string `json:"utilsBaseURL,omitempty"`
	}

	stackDetails := &AmbariRepoConfig{
		Version:      "____",
		StackBaseURL: "____",
		GpgKeyURL:    "____",
	}

	jsonBytes, err := json.MarshalIndent(stackDetails, "", "\t")
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	fmt.Printf("%s\n", string(jsonBytes))
}
