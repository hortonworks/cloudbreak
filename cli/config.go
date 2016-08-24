package cli

import (
	"encoding/json"
	"fmt"
	"github.com/urfave/cli"
	"gopkg.in/yaml.v2"
	"log"
	"os"
	"os/user"
)

type Config struct {
	Username string `json:"username" yaml:"username"`
	Password string `json:"password" yaml:"password"`
	Server   string `json:"server" yaml:"server"`
}

func (c Config) Json() string {
	j, _ := json.Marshal(c)
	return string(j)
}

func (c Config) Yaml() string {
	j, _ := yaml.Marshal(c)
	return string(j)
}

func Configure(c *cli.Context) error {
	if c.NumFlags() != 3 || len(c.String(FlCBUsername.Name)) == 0 || len(c.String(FlCBPassword.Name)) == 0 || len(c.String(FlCBServer.Name)) == 0 {
		return cli.NewExitError(fmt.Sprintf("You need to specify all the parameters. See '%s configure --help'.", c.App.Name), 1)
	}

	err := WriteCredentialsToFile(c.String(FlCBServer.Name), c.String(FlCBUsername.Name), c.String(FlCBPassword.Name))
	if err != nil {
		log.Print(fmt.Sprintf("[WriteCredentialsToFile] %s", err.Error()))
		os.Exit(1)
	}
	return nil
}

func WriteCredentialsToFile(server string, username string, password string) error {
	currentUser, err := user.Current()
	if err != nil {
		return err
	}
	log.Printf("[WriteCredentialsToFile] current user: %s", currentUser.Username)

	hdcDir := currentUser.HomeDir + "/" + hdc_dir
	configFile := hdcDir + "/" + config_file

	if _, err := os.Stat(hdcDir); os.IsNotExist(err) {
		log.Printf("[WriteCredentialsToFile] create dir: %s", hdcDir)
		err = os.MkdirAll(hdcDir, 0744)
		if err != nil {
			return err
		}
	} else {
		log.Printf("[WriteCredentialsToFile] dir already exists: %s", hdcDir)
	}

	if _, err := os.Stat(configFile); os.IsNotExist(err) {
		log.Printf("[WriteCredentialsToFile] create file: %s", configFile)
		if _, err := os.Create(configFile); err != nil {
			return err
		}
	} else {
		log.Printf("[WriteCredentialsToFile] file already exists: %s", configFile)
	}

	f, err := os.OpenFile(configFile, os.O_WRONLY, 0600)
	if err != nil {
		return err
	}

	log.Printf("[WriteCredentialsToFile] writing credentials to file: %s", configFile)
	confJson := Config{Server: server, Username: username, Password: password}.Yaml()
	if _, err = f.WriteString(confJson); err != nil {
		return err
	}
	f.Close()

	return nil
}
