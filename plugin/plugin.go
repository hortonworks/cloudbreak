package plugin

import (
	"bufio"
	"fmt"
	log "github.com/Sirupsen/logrus"
	"io"
	"os"
	"os/exec"
	"runtime"
	"strings"
	"sync"
)

func DelegateCommand(subCmd string, argsIndex int, exitFunc func(int)) {
	var command *exec.Cmd
	if runtime.GOOS == "windows" {
		arr := []string{"/C", subCmd}
		arr = append(arr, os.Args[argsIndex:]...)
		command = exec.Command("cmd", arr...)
	} else {
		command = exec.Command(subCmd, os.Args[argsIndex:]...)
	}
	command.Stdin = os.Stdin
	stdOut, _ := command.StdoutPipe()
	stdErr, _ := command.StderrPipe()
	err := command.Start()
	if err != nil {
		log.Fatalf("Error executing the plugin: %s", err.Error())
	}
	var wg sync.WaitGroup
	wg.Add(2)
	go printOut(&wg, stdOut, true)
	go printOut(&wg, stdErr, false)
	err = command.Wait()
	if err != nil {
		// ignore since the error response is a valid response from the subprocess
	}
	wg.Wait()
	exitFunc(0)
}

func printOut(wg *sync.WaitGroup, pipe io.ReadCloser, stdOut bool) {
	defer wg.Done()
	scanner := bufio.NewScanner(pipe)
	scanner.Split(bufio.ScanLines)
	for scanner.Scan() {
		if stdOut {
			fmt.Println(scanner.Text())
		} else {
			text := scanner.Text()
			msg := text
			if strings.Contains(text, "msg=\"") {
				msg = text[strings.Index(text, "msg=\"")+5 : strings.LastIndex(text, "\"")]
			}
			if strings.Contains(text, "debug") {
				log.Debug(msg)
			} else if strings.Contains(text, "error") {
				log.Error(msg)
			} else {
				log.Info(msg)
			}
		}
	}
}
