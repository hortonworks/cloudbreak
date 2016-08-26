package cli

import (
	"github.com/olekukonko/tablewriter"
	"os"
)

type TableRow interface {
	DataAsStringArray() []string
}

func WriteTable(header []string, tableRows []TableRow) {
	table := tablewriter.NewWriter(os.Stdout)
	table.SetHeader(header)
	for _, v := range tableRows {
		table.Append(v.DataAsStringArray())
	}
	table.Render() // Send output
}
