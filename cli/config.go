package cli

import (
	"encoding/json"
	"io/ioutil"
	"os"

	"errors"

	"fmt"
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/mitchellh/go-homedir"
	"github.com/urfave/cli"
	"gopkg.in/yaml.v2"
)

const (
	Config_dir  = ".cb"
	Config_file = "config"
)

type Config struct {
	Username string `json:"username" yaml:"username"`
	Password string `json:"password,omitempty" yaml:"password,omitempty"`
	Server   string `json:"server" yaml:"server"`
	Output   string `json:"output,omitempty" yaml:"output,omitempty"`
}

type ConfigList map[string]Config

func (c Config) Yaml() string {
	j, _ := yaml.Marshal(c)
	return string(j)
}

func (c ConfigList) Json() string {
	j, _ := json.MarshalIndent(c, "", "  ")
	return string(j)
}

func (c ConfigList) Yaml() string {
	j, _ := yaml.Marshal(c)
	return string(j)
}

func Configure(c *cli.Context) {
	checkRequiredFlags(c)

	err := writeConfigToFile(GetHomeDirectory(), c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name), c.String(FlOutput.Name), c.String(FlProfile.Name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
}

func GetHomeDirectory() string {
	homeDir, err := homedir.Dir()
	if err != nil || len(homeDir) == 0 {
		utils.LogErrorAndExit(errors.New("failed to determine the home directory"))
	}
	return homeDir
}

func ReadConfig(baseDir string, profile string) (*Config, error) {
	configDir := baseDir + "/" + Config_dir
	configFile := configDir + "/" + Config_file

	if _, err := os.Stat(configFile); os.IsNotExist(err) {
		return nil, err
	}
	log.Infof("[ReadConfig] found config file: %s", configFile)

	content, err := ioutil.ReadFile(configFile)
	if err != nil {
		return nil, err
	}

	var configList ConfigList
	err = yaml.Unmarshal(content, &configList)
	if err != nil {
		return nil, err
	}

	if config, valid := configList[profile]; valid {
		log.Infof("[ReadConfig] selected profile: %s", profile)
		return &config, nil
	} else {
		return nil, errors.New(fmt.Sprintf("Invalid profile selected: %s", profile))
	}
}

func writeConfigToFile(baseDir string, server string, username string, password string, output string, profile string) error {
	configDir := baseDir + "/" + Config_dir
	configFile := configDir + "/" + Config_file
	if len(profile) == 0 {
		profile = "default"
	}

	if _, err := os.Stat(configDir); os.IsNotExist(err) {
		log.Infof("[writeConfigToFile] create dir: %s", configDir)
		err = os.MkdirAll(configDir, 0700)
		if err != nil {
			return err
		}
	} else {
		log.Infof("[writeConfigToFile] dir already exists: %s", configDir)
	}

	log.Infof("[writeConfigToFile] writing credentials to file: %s", configFile)

	configs := ConfigList{
		profile: Config{Server: server, Username: username, Password: password, Output: output},
	}

	f, err := os.OpenFile(configFile, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0600)
	if err != nil {
		return err
	}
	defer f.Close()
	if _, err := f.Write([]byte(configs.Yaml())); err != nil {
		return err
	}

	return nil
}
