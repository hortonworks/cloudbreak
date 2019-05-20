package maintenancemode

import (
	"encoding/json"
	"fmt"
	"github.com/Sirupsen/logrus"
	v4maint "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_stacks"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/cb-cli/dataplane/types"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

type maintenanceModeClient interface {
	SetClusterMaintenanceMode(params *v4maint.SetClusterMaintenanceModeParams) error
	PutClusterV4(params *v4maint.PutClusterV4Params) error
}

func EnableMaintenanceMode(c *cli.Context) {
	logrus.Infof("[EnableMaintenanceMode] enable maintenance mode")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	workspaceID, name := resolveWorkspaceIDAndStackName(c.Int64, c.String)
	toggleMaintenanceMode(cbClient.Cloudbreak.V4WorkspaceIDStacks, workspaceID, name, model.MaintenanceModeV4RequestStatusENABLED)
}

func resolveWorkspaceIDAndStackName(int64Finder func(string) int64, stringFinder func(string) string) (int64, string) {
	return int64Finder(flags.FlWorkspaceOptional.Name), stringFinder(flags.FlName.Name)
}

func toggleMaintenanceMode(client maintenanceModeClient, workspaceId int64, name string, status string) {
	request := &model.MaintenanceModeV4Request{Status: status}
	err := client.SetClusterMaintenanceMode(v4maint.NewSetClusterMaintenanceModeParams().WithWorkspaceID(workspaceId).WithName(name).WithBody(request))
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
	toggleMaintenanceMode(cbClient.Cloudbreak.V4WorkspaceIDStacks, workspaceID, name, model.MaintenanceModeV4RequestStatusDISABLED)
}

func ValidateRepositoryConfigurations(c *cli.Context) {
	logrus.Infof("[ValidateRepositoryConfigurations] validate repository configurations")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	workspaceID, name := resolveWorkspaceIDAndStackName(c.Int64, c.String)
	toggleMaintenanceMode(cbClient.Cloudbreak.V4WorkspaceIDStacks, workspaceID, name, model.MaintenanceModeV4RequestStatusVALIDATIONREQUESTED)
}

func ChangeHdpRepo(c *cli.Context) {
	logrus.Infof("[ChangeHdpRepo] change HDP repo")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	workspaceID, name := resolveWorkspaceIDAndStackName(c.Int64, c.String)

	stackDetails := &model.StackRepositoryV4Request{
		Version:                  &(&types.S{S: c.String(flags.FlVersion.Name)}).S,
		VersionDefinitionFileURL: c.String(flags.FlVdfUrl.Name),
	}

	unmarshallCliInput(c.String, stackDetails)
	stackDetails.Stack = &(&types.S{S: "HDP"}).S
	updateStackRepoDetails(cbClient.Cloudbreak.V4WorkspaceIDStacks, workspaceID, name, stackDetails)
}

func unmarshallCliInput(stringFinder func(string) string, stackDetails *model.StackRepositoryV4Request) {
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

	stackDetails := &model.StackRepositoryV4Request{
		Version:                  &(&types.S{S: c.String(flags.FlVersion.Name)}).S,
		VersionDefinitionFileURL: c.String(flags.FlVdfUrl.Name),
		MpackURL:                 c.String(flags.FlMPackUrl.Name),
	}

	unmarshallCliInput(c.String, stackDetails)
	stackDetails.Stack = &(&types.S{S: "HDF"}).S
	updateStackRepoDetails(cbClient.Cloudbreak.V4WorkspaceIDStacks, workspaceID, name, stackDetails)

}
func ChangeAmbariRepo(c *cli.Context) {
	logrus.Infof("[ChangeAmbariRepo] change Ambari repo")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	workspaceID, name := resolveWorkspaceIDAndStackName(c.Int64, c.String)

	stackDetails := &model.StackRepositoryV4Request{
		Version: &(&types.S{S: c.String(flags.FlVersion.Name)}).S,
		Repository: &model.RepositoryV4Request{
			BaseURL:   c.String(flags.FlRepoUrl.Name),
			GpgKeyURL: c.String(flags.FlRepoGpgUrl.Name),
		},
	}

	unmarshallCliInput(c.String, stackDetails)
	stackDetails.Stack = &(&types.S{S: "AMBARI"}).S
	updateStackRepoDetails(cbClient.Cloudbreak.V4WorkspaceIDStacks, workspaceID, name, stackDetails)
}

func updateStackRepoDetails(client maintenanceModeClient, workspaceID int64, name string, stackDetails *model.StackRepositoryV4Request) {
	request := &model.UpdateClusterV4Request{
		StackRepository: stackDetails,
	}
	err := client.PutClusterV4(v4maint.NewPutClusterV4Params().WithWorkspaceID(workspaceID).WithName(name).WithBody(request))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
}

func GenerateHdpRepoJson(_ *cli.Context) {
	stackDetails := &model.StackRepositoryV4Request{
		RepoID: "____",
		OsType: "____",
		Repository: &model.RepositoryV4Request{
			BaseURL: "____",
			Version: &(&types.S{S: "____"}).S,
		},
		VersionDefinitionFileURL: "____",
		UtilsRepoID:              "____",
		UtilsBaseURL:             "____",
		EnableGplRepo:            true,
		Mpacks:                   []*model.ManagementPackDetailsV4Request{},
		Version:                  &(&types.S{S: "____"}).S,
	}

	jsonBytes, err := json.MarshalIndent(stackDetails, "", "\t")
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	fmt.Printf("%s\n", string(jsonBytes))
}

func GenerateHdfRepoJson(_ *cli.Context) {
	stackDetails := &model.StackRepositoryV4Request{
		RepoID: "____",
		OsType: "____",
		Repository: &model.RepositoryV4Request{
			BaseURL: "____",
			Version: &(&types.S{S: "____"}).S,
		},
		VersionDefinitionFileURL: "____",
		UtilsRepoID:              "____",
		UtilsBaseURL:             "____",
		EnableGplRepo:            true,
		MpackURL:                 "____",
		Mpacks: []*model.ManagementPackDetailsV4Request{
			{
				Name:         &(&types.S{S: "____"}).S,
				PreInstalled: false,
			},
		},
		Version: &(&types.S{S: "____"}).S,
	}

	jsonBytes, err := json.MarshalIndent(stackDetails, "", "\t")
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	fmt.Printf("%s\n", string(jsonBytes))
}

func GenerateAmbariRepoJson(_ *cli.Context) {
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
