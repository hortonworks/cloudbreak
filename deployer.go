package main

import (
	"fmt"
	"os"

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

func main() {
	if len(os.Args) == 2 && os.Args[1] == "--version" {
		fmt.Println("CloudBreak Deployer:", version())
		os.Exit(0)
	}

	basher.Application(map[string]func([]string){
		"hello":       Hello,
		"bin-version": BinVersion,
	}, []string{
		"include/cloudbreak.bash",
		"include/cmd.bash",
		"include/color.bash",
		"include/deps.bash",
		"include/env.bash",
		"include/fn.bash",
		"include/module.bash",
	}, Asset, false)

}
