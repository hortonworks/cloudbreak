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
	AuthType string `json:"authType,omitempty" yaml:"authType,omitempty"`
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
	checkRequiredFlagsAndArguments(c)

	err := writeConfigToFile(GetHomeDirectory(), c.String(FlServerOptional.Name),
		c.String(FlUsername.Name), c.String(FlPassword.Name),
		c.String(FlOutputOptional.Name), c.String(FlProfileOptional.Name), c.String(FlAuthTypeOptional.Name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
}

func GetHomeDirectory() string {
	homeDir, err := homedir.Dir()
	if err != nil || len(homeDir) == 0 {
		utils.LogErrorMessageAndExit("failed to determine the home directory")
	}
	return homeDir
}

func ReadConfig(baseDir string, profile string) (*Config, error) {
	configDir := baseDir + "/" + Config_dir
	configFile := configDir + "/" + Config_file

	if _, err := os.Stat(configFile); os.IsNotExist(err) {
		return nil, fmt.Errorf("%s, first create a config by executing `cb configure command`", err.Error())
	}
	log.Infof("[ReadConfig] found config file: %s", configFile)

	var configList ConfigList
	err := readConfigToList(configFile, &configList)
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

func writeConfigToFile(baseDir, server, username, password, output, profile, authType string) error {
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

	var configList = make(ConfigList)
	if _, err := os.Stat(configFile); !os.IsNotExist(err) {
		err = readConfigToList(configFile, &configList)
		if err != nil {
			return err
		}
	}

	configList[profile] = Config{Server: server, Username: username, Password: password, Output: output, AuthType: authType}

	err := ioutil.WriteFile(configFile, []byte(configList.Yaml()), 0600)
	if err != nil {
		return err
	}

	return nil
}

func readConfigToList(configPath string, configList *ConfigList) error {
	content, err := ioutil.ReadFile(configPath)
	if err != nil {
		return err
	}
	err = yaml.Unmarshal(content, configList)
	if err != nil {
		return err
	}
	return nil
}
