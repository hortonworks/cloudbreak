package config

import (
	"encoding/json"
	"errors"
	"fmt"
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cloudbreak/common"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	ws "github.com/hortonworks/cb-cli/cloudbreak/workspace"
	"github.com/hortonworks/cb-cli/utils"
	"github.com/mitchellh/go-homedir"
	"github.com/urfave/cli"
	"golang.org/x/crypto/ssh/terminal"
	"gopkg.in/yaml.v2"
	"io/ioutil"
	"os"
	"strconv"
	"syscall"
)

type Config struct {
	Username  string `json:"username" yaml:"username"`
	Password  string `json:"password,omitempty" yaml:"password,omitempty"`
	Server    string `json:"server" yaml:"server"`
	AuthType  string `json:"authType,omitempty" yaml:"authType,omitempty"`
	Workspace string `json:"workspace,omitempty" yaml:"workspace,omitempty"`
	Output    string `json:"output,omitempty" yaml:"output,omitempty"`
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

func CheckConfigAndCommandFlags(c *cli.Context) error {
	err := ConfigRead(c)
	if err == nil {
		return fl.CheckRequiredFlagsAndArguments(c)
	}
	return err
}

func ConfigRead(c *cli.Context) error {
	args := c.Args()
	if args.Present() {
		name := args.First()
		if k := c.App.Command(name); k != nil {
			// this is a sub-command invocation
			return nil
		}
	}

	server := c.String(fl.FlServerOptional.Name)
	username := c.String(fl.FlUsername.Name)
	password := c.String(fl.FlPassword.Name)
	output := c.String(fl.FlOutputOptional.Name)
	profile := c.String(fl.FlProfileOptional.Name)
	authType := c.String(fl.FlAuthTypeOptional.Name)
	workspace := c.String(fl.FlWorkspaceOptional.Name)

	if len(profile) == 0 {
		profile = "default"
	}

	config, err := ReadConfig(GetHomeDirectory(), profile)
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	set := func(name, value string) {
		if err = c.Set(name, value); err != nil {
			log.Debug(err)
		}
	}

	if len(output) == 0 {
		set(fl.FlOutputOptional.Name, config.Output)
	}

	if len(authType) == 0 {
		authType = config.AuthType
		if len(authType) == 0 {
			set(fl.FlAuthTypeOptional.Name, common.OAUTH2)
		} else {
			if authType != common.OAUTH2 && authType != common.BASIC {
				utils.LogErrorAndExit(errors.New(fmt.Sprintf("invalid authentication type, accepted values: [%s, %s]", common.OAUTH2, common.BASIC)))
			}
			set(fl.FlAuthTypeOptional.Name, authType)
		}
	}
	PrintConfig(*config)
	if len(server) == 0 {
		set(fl.FlServerOptional.Name, config.Server)
	}
	if len(username) == 0 {
		set(fl.FlUsername.Name, config.Username)
	}
	if len(password) == 0 {
		if len(config.Password) == 0 {
			fmt.Print("Enter Password: ")
			bytePassword, ioErr := terminal.ReadPassword(int(syscall.Stdin))
			fmt.Println()
			if ioErr != nil {
				utils.LogErrorAndExit(ioErr)
			}
			set(fl.FlPassword.Name, string(bytePassword))
		} else {
			set(fl.FlPassword.Name, config.Password)
		}
	}
	if len(workspace) == 0 {
		if len(config.Workspace) == 0 {
			workspaceList := ws.GetWorkspaceList(c)
			var workspaceID string
			for _, workspace := range workspaceList {
				if workspace.Name == c.String(fl.FlUsername.Name) {
					workspaceID = strconv.FormatInt(workspace.ID, 10)
				}
			}

			err = WriteConfigToFile(GetHomeDirectory(), config.Server, config.Username, config.Password, config.Output, profile, config.AuthType, config.Username)
			if err != nil {
				utils.LogErrorAndExit(err)
			}
			set(fl.FlWorkspaceOptional.Name, workspaceID)
		} else {
			workspaceID := ws.GetWorkspaceIdByName(c, config.Workspace)
			set(fl.FlWorkspaceOptional.Name, strconv.FormatInt(workspaceID, 10))
		}
	} else {
		workspaceID := ws.GetWorkspaceIdByName(c, workspace)
		set(fl.FlWorkspaceOptional.Name, strconv.FormatInt(workspaceID, 10))
	}

	server = c.String(fl.FlServerOptional.Name)
	username = c.String(fl.FlUsername.Name)
	password = c.String(fl.FlPassword.Name)
	workspace = c.String(fl.FlWorkspaceOptional.Name)
	if len(server) == 0 || len(username) == 0 || len(password) == 0 || len(workspace) == 0 {
		log.Error(fmt.Sprintf("configuration is not set, see: cb configure --help or provide the following flags: %v",
			[]string{"--" + fl.FlServerOptional.Name, "--" + fl.FlUsername.Name, "--" + fl.FlPassword.Name, "--" + fl.FlWorkspaceOptional.Name}))
		os.Exit(1)
	}
	return nil
}

func PrintConfig(cfg Config) {
	cfg.Password = "*"
	log.Infof("[CheckConfigAndCommandFlags] Config read from file, setting as global variable:\n%s", cfg.Yaml())
}

func GetHomeDirectory() string {
	homeDir, err := homedir.Dir()
	if err != nil || len(homeDir) == 0 {
		utils.LogErrorMessageAndExit("failed to determine the home directory")
	}
	return homeDir
}

func ReadConfig(baseDir string, profile string) (*Config, error) {
	configDir := baseDir + "/" + common.Config_dir
	configFile := configDir + "/" + common.Config_file

	if _, err := os.Stat(configFile); os.IsNotExist(err) {
		return new(Config), nil
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

func WriteConfigToFile(baseDir, server, username, password, output, profile, authType, workspace string) error {
	configDir := baseDir + "/" + common.Config_dir
	configFile := configDir + "/" + common.Config_file
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

	configList[profile] = Config{Server: server,
		Workspace: workspace,
		Username:  username,
		Password:  password,
		Output:    output,
		AuthType:  authType,
	}

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
