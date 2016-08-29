package cli

import (
	"github.com/olekukonko/tablewriter"
	"os"
)

type GenericRow struct {
	Data []string
}

func (r *GenericRow) DataAsStringArray() []string {
	return r.Data
}

type TableRow interface {
	DataAsStringArray() []string
}

func WriteTable(header []string, tableRows []TableRow) {
	table := tablewriter.NewWriter(os.Stdout)
	table.SetHeader(header)
	table.SetAutoWrapText(false)
	for _, v := range tableRows {
		table.Append(v.DataAsStringArray())
	}
	table.Render() // Send output
}
