package userprofile

import (
	"strconv"

	log "github.com/Sirupsen/logrus"
	v4img "github.com/hortonworks/cb-cli/dataplane/api/client/v4user_profiles"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

var showClustersPreferencesHeader = []string{"Show terminated clusters", "Source of config", "Timeout (days)", "Timeout (hours)", "Timeout (minutes)", "Timeout (seconds)"}

type showClustersPreferencesOut struct {
	ShowTerminatedClustersActive bool   `json:"Show terminated active" yaml:"Show terminated active"`
	Source                       string `json:"Source of config" yaml:"Source of config"`
	TimeoutDays                  int32  `json:"Show terminated clusters timeout (days)" yaml:"Show terminated clusters timeout (days)"`
	TimeoutHours                 int32  `json:"Show terminated clusters timeout (hours)" yaml:"Show terminated clusters timeout (hours)"`
	TimeoutMinutes               int32  `json:"Show terminated clusters timeout (minutes)" yaml:"Show terminated clusters timeout (minutes)"`
}

func (r *showClustersPreferencesOut) DataAsStringArray() []string {
	return []string{
		strconv.FormatBool(r.ShowTerminatedClustersActive),
		r.Source,
		strconv.FormatInt(int64(r.TimeoutDays), 10),
		strconv.FormatInt(int64(r.TimeoutHours), 10),
		strconv.FormatInt(int64(r.TimeoutMinutes), 10),
	}
}

func GetShowTerminatedClustersPreferences(c *cli.Context) {
	log.Infof("[SetShowClustersPreferences] Set show cluster preferences for a user")

	userprofileClient := oauth.NewCloudbreakHTTPClientFromContext(c).Cloudbreak.V4userProfiles
	resp, err := userprofileClient.GetTerminatedClustersPreferences(v4img.NewGetTerminatedClustersPreferencesParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	output.Write(showClustersPreferencesHeader, &showClustersPreferencesOut{
		*resp.Payload.Active,
		resp.Payload.Source,
		*resp.Payload.Timeout.Days,
		*resp.Payload.Timeout.Hours,
		*resp.Payload.Timeout.Minutes,
	})
}

func ActivateShowTerminatedClusters(c *cli.Context) {
	log.Infof("[ActivateShowTerminatedClusters] Turn on show terminated cluster preferences for a user")

	timeout := getTimeoutRequestFromCommandLine(c)
	showClustersRequest := &model.ShowTerminatedClustersPreferencesV4Request{
		Active:  true,
		Timeout: timeout,
	}

	userprofileClient := oauth.NewCloudbreakHTTPClientFromContext(c).Cloudbreak.V4userProfiles
	err := userprofileClient.PutTerminatedClustersPreferences(v4img.NewPutTerminatedClustersPreferencesParams().WithBody(showClustersRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
}

func DectivateShowTerminatedClusters(c *cli.Context) {
	log.Infof("[DectivateShowTerminatedClusters] Turn off show terminated clusters for a user")

	showClustersRequest := &model.ShowTerminatedClustersPreferencesV4Request{
		Active:  false,
		Timeout: nil,
	}

	userprofileClient := oauth.NewCloudbreakHTTPClientFromContext(c).Cloudbreak.V4userProfiles
	err := userprofileClient.PutTerminatedClustersPreferences(v4img.NewPutTerminatedClustersPreferencesParams().WithBody(showClustersRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
}

func DeleteShowTerminatedClustersPreferences(c *cli.Context) {
	log.Infof("[DeleteShowTerminatedClustersPreferences] Delete show terminated cluster preferences for a user")

	userprofileClient := oauth.NewCloudbreakHTTPClientFromContext(c).Cloudbreak.V4userProfiles
	err := userprofileClient.DeleteTerminatedClustersPreferences(v4img.NewDeleteTerminatedClustersPreferencesParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}
}

func retrieveShowClustersPreferences(c *cli.Context) *v4img.GetTerminatedClustersPreferencesOK {
	userprofileClient := oauth.NewCloudbreakHTTPClientFromContext(c).Cloudbreak.V4userProfiles
	resp, errGet := userprofileClient.GetTerminatedClustersPreferences(v4img.NewGetTerminatedClustersPreferencesParams())
	if errGet != nil {
		utils.LogErrorAndExit(errGet)
	}

	return resp
}

func getTimeoutRequestFromCommandLine(c *cli.Context) *model.DurationV4Request {
	minutes := int32(c.Int(fl.FlTimeoutMinutes.Name))
	hours := int32(c.Int(fl.FlTimeoutHours.Name))
	days := int32(c.Int(fl.FlTimeoutDays.Name))

	if minutes == 0 && hours == 0 && days == 0 {
		return nil
	}
	timeout := model.DurationV4Request{
		Minutes: &minutes,
		Hours:   &hours,
		Days:    &days,
	}

	return &timeout
}
