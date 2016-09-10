package cli

import (
	"encoding/json"
	"fmt"
	"github.com/olekukonko/tablewriter"
	"gopkg.in/yaml.v2"
	"os"
)

type Output struct {
	Format string
}

func (o *Output) Write(header []string, row Row) {
	StopSpinner()
	if o.Format == "table" {
		writeTable(header, []Row{row})
	} else if o.Format == "yaml" {
		y, _ := yaml.Marshal(row)
		fmt.Println(string(y))
	} else {
		j, _ := json.MarshalIndent(row, "", "  ")
		fmt.Println(string(j))
	}
}

func (o *Output) WriteList(header []string, tableRows []Row) {
	StopSpinner()
	if o.Format == "table" {
		writeTable(header, tableRows)
	} else if o.Format == "yaml" {
		y, _ := yaml.Marshal(tableRows)
		fmt.Println(string(y))
	} else {
		j, _ := json.MarshalIndent(tableRows, "", "  ")
		fmt.Println(string(j))
	}
}

type Row interface {
	DataAsStringArray() []string
}

func writeTable(header []string, tableRows []Row) {
	table := tablewriter.NewWriter(os.Stdout)
	table.SetHeader(header)
	table.SetAutoWrapText(false)
	for _, v := range tableRows {
		table.Append(v.DataAsStringArray())
	}
	table.Render() // Send output
}
