package cli

import (
	"encoding/json"
	"io/ioutil"
	"os"

	"errors"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/cli/utils"
	"github.com/mitchellh/go-homedir"
	"github.com/urfave/cli"
	"gopkg.in/yaml.v2"
)

const (
	Hdc_dir     = ".hdc"
	Config_file = "config"
)

type Config struct {
	Username string `json:"username" yaml:"username"`
	Password string `json:"password,omitempty" yaml:"password,omitempty"`
	Server   string `json:"server" yaml:"server"`
	Output   string `json:"output,omitempty" yaml:"output,omitempty"`
}

func (c Config) Json() string {
	j, _ := json.MarshalIndent(c, "", "  ")
	return string(j)
}

func (c Config) Yaml() string {
	j, _ := yaml.Marshal(c)
	return string(j)
}

func Configure(c *cli.Context) error {
	checkRequiredFlags(c)

	err := WriteConfigToFile(GetHomeDirectory(), c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name), c.String(FlOutput.Name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return nil
}

func GetHomeDirectory() string {
	homeDir, err := homedir.Dir()
	if err != nil || len(homeDir) == 0 {
		utils.LogErrorAndExit(errors.New("failed to determine the home directory"))
	}
	return homeDir
}

func WriteConfigToFile(baseDir string, server string, username string, password string, output string) error {
	hdcDir := baseDir + "/" + Hdc_dir
	configFile := hdcDir + "/" + Config_file

	if _, err := os.Stat(hdcDir); os.IsNotExist(err) {
		log.Infof("[WriteCredentialsToFile] create dir: %s", hdcDir)
		err = os.MkdirAll(hdcDir, 0700)
		if err != nil {
			return err
		}
	} else {
		log.Infof("[WriteConfigToFile] dir already exists: %s", hdcDir)
	}

	log.Infof("[WriteConfigToFile] writing credentials to file: %s", configFile)
	confJson := Config{Server: server, Username: username, Password: password, Output: output}.Yaml()
	err := ioutil.WriteFile(configFile, []byte(confJson), 0600)
	if err != nil {
		return err
	}

	return nil
}

func ReadConfig(baseDir string) (*Config, error) {
	hdcDir := baseDir + "/" + Hdc_dir
	configFile := hdcDir + "/" + Config_file

	if _, err := os.Stat(configFile); os.IsNotExist(err) {
		return nil, err
	}
	log.Infof("[ReadConfig] found config file: %s", configFile)

	content, err := ioutil.ReadFile(configFile)
	if err != nil {
		return nil, err
	}
	var config Config
	err = yaml.Unmarshal(content, &config)
	if err != nil {
		return nil, err
	}

	return &config, nil
}
