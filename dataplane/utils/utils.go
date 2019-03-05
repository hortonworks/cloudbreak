package utils

import (
	"errors"
	"github.com/hortonworks/cb-cli/dataplane/api/model"

	"github.com/hortonworks/cb-cli/dataplane/api/client/v4utils"
	commonutils "github.com/hortonworks/dp-cli-common/utils"
)

type utilClient interface {
	CheckClientVersionV4(params *v4utils.CheckClientVersionV4Params) (*v4utils.CheckClientVersionV4OK, error)
}

func CheckClientVersion(client utilClient, version string) {
	resp, err := client.CheckClientVersionV4(v4utils.NewCheckClientVersionV4Params().WithVersion(&version))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	valid := resp.Payload.VersionCheckOk
	message := resp.Payload.Message
	if valid == nil || !*valid {
		commonutils.LogErrorAndExit(errors.New(message))
	}
}

func SafeCloudPlatformConvert(env *model.EnvironmentSettingsV4Response) string {
	if env != nil {
		return env.CloudPlatform
	}
	return ""
}

func SafeEnvironmentNameConvert(env *model.EnvironmentSettingsV4Response) string {
	if env != nil {
		return env.Name
	}
	return ""
}

func SafeClusterDescriptionConvert(s *model.StackV4Response) string {
	if s.Cluster != nil {
		return s.Cluster.Description
	}
	return ""
}

func SafeClusterViewStatusConvert(s *model.StackViewV4Response) string {
	if s.Cluster != nil {
		return s.Cluster.Status
	}
	return ""
}

func SafeClusterStatusConvert(s *model.StackV4Response) string {
	if s.Cluster != nil {
		return s.Cluster.Status
	}
	return ""
}
