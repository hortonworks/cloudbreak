package main

import (
	"fmt"
	"log"
	"os"
	"os/exec"
	"strings"

	"github.com/progrium/go-basher"
)

var Version string
var GitRevision string

func version() (v string) {
	if GitRevision == "" {
		v = Version
	} else {
		v = fmt.Sprintf("%s-%s", Version, GitRevision)
	}
	return
}

func Hello(args []string) {
	fmt.Println("Hello from golang")
}

func BinVersion(args []string) {
	fmt.Println(version())
}

// Copied from https://github.com/progrium/go-basher/blob/master/basher.go#L37
// to be able to add exporting DEBUG and TRACE, as we don't inherit anything
// from parent bash

func application(
	funcs map[string]func([]string),
	scripts []string,
	loader func(string) ([]byte, error),
	copyEnv bool) {

	var bashPath string
	bashPath, err := exec.LookPath("bash")
	if err != nil {
		if strings.Contains(os.Getenv("SHELL"), "bash") {
			bashPath = os.Getenv("SHELL")
		} else {
			bashPath = "/bin/bash"
		}
	}
	bash, err := basher.NewContext(bashPath, os.Getenv("DEBUG") != "")

	bash.Export("DEBUG", os.Getenv("DEBUG"))
	bash.Export("TRACE", os.Getenv("TRACE"))

	if err != nil {
		log.Fatal(err)
	}
	for name, fn := range funcs {
		bash.ExportFunc(name, fn)
	}
	if bash.HandleFuncs(os.Args) {
		os.Exit(0)
	}

	for _, script := range scripts {
		bash.Source(script, loader)
	}
	if copyEnv {
		bash.CopyEnv()
	}
	status, err := bash.Run("main", os.Args[1:])
	if err != nil {
		log.Fatal(err)
	}
	os.Exit(status)
}

func main() {
	if len(os.Args) == 2 && os.Args[1] == "--version" {
		fmt.Println("CloudBreak Deployer:", version())
		os.Exit(0)
	}

	application(map[string]func([]string){
		"hello":       Hello,
		"bin-version": BinVersion,
	}, []string{
		"include/circle.bash",
		"include/cmd.bash",
		"include/color.bash",
		"include/deployer.bash",
		"include/deps.bash",
		"include/docker.bash",
		"include/env.bash",
		"include/fn.bash",
		"include/module.bash",
	}, Asset, false)

}
