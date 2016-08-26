package cli

import (
	"encoding/json"
	"fmt"
	log "github.com/Sirupsen/logrus"
	"github.com/urfave/cli"
	"gopkg.in/yaml.v2"
	"io/ioutil"
	"os"
	"os/user"
)

type Config struct {
	Username string `json:"username" yaml:"username"`
	Password string `json:"password" yaml:"password"`
	Server   string `json:"server" yaml:"server"`
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
	if c.NumFlags() != 3 || len(c.String(FlCBUsername.Name)) == 0 || len(c.String(FlCBPassword.Name)) == 0 || len(c.String(FlCBServer.Name)) == 0 {
		log.Error(fmt.Sprintf("You need to specify all the parameters. See '%s configure --help'.", c.App.Name))
		return newExitError()
	}

	err := WriteConfigToFile(c.String(FlCBServer.Name), c.String(FlCBUsername.Name), c.String(FlCBPassword.Name))
	if err != nil {
		log.Error(fmt.Sprintf("[WriteConfigToFile] %s", err.Error()))
		return newExitError()
	}
	return nil
}

func GetCurrentUser() *user.User {
	currentUser, err := user.Current()
	if err != nil {
		log.Fatalf("[GetCurrentUser] %s", err.Error())
		os.Exit(1)
	}
	return currentUser
}

func WriteConfigToFile(server string, username string, password string) error {
	currentUser := GetCurrentUser()
	log.Infof("[WriteConfigToFile] current user: %s", currentUser.Username)

	hdcDir := currentUser.HomeDir + "/" + Hdc_dir
	configFile := hdcDir + "/" + Config_file

	if _, err := os.Stat(hdcDir); os.IsNotExist(err) {
		log.Infof("[WriteCredentialsToFile] create dir: %s", hdcDir)
		err = os.MkdirAll(hdcDir, 0744)
		if err != nil {
			return err
		}
	} else {
		log.Infof("[WriteConfigToFile] dir already exists: %s", hdcDir)
	}

	log.Infof("[WriteConfigToFile] writing credentials to file: %s", configFile)
	confJson := Config{Server: server, Username: username, Password: password}.Yaml()
	err := ioutil.WriteFile(configFile, []byte(confJson), 0744)
	if err != nil {
		return err
	}

	return nil
}

func ReadConfig() (*Config, error) {
	currentUser := GetCurrentUser()
	log.Infof("[ReadConfig] current user: %s", currentUser.Username)

	hdcDir := currentUser.HomeDir + "/" + Hdc_dir
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
