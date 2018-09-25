package completion

import (
	"fmt"
	"github.com/urfave/cli"
)

var bashCompletionTemplate = `_cli_bash_autocomplete() {
     local cur opts base
     COMPREPLY=()
     cur="${COMP_WORDS[COMP_CWORD]}"
     base=${COMP_WORDS[@]:0:$COMP_CWORD}
     wrd_num=$(echo $base | wc -w | awk '{print $1}')
     word=$(echo $base | awk "{print \$$wrd_num}")
     if [[ ! "$word" == --* ]]; then
       opts=$( ${base} --generate-bash-completion )
       COMPREPLY=( $(compgen -W "${opts}" -- ${cur}) )
     else
       COMPREPLY=()
     fi
     return 0
}

complete -o default -F _cli_bash_autocomplete %s
`

func PrintBashCompletion(c *cli.Context) error {
	appName := c.App.Name
	bashCompletion := fmt.Sprintf(bashCompletionTemplate, appName)
	fmt.Print(bashCompletion)
	fmt.Printf(`
# Run one of these commands to configure your shell:
# eval "$(%s completion)"
# source <(%s completion)
`, appName, appName)
	return nil
}
