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
	if o.Format == "table" {
		WriteTable(header, []Row{row})
	} else if o.Format == "yaml" {
		y, _ := yaml.Marshal(row)
		fmt.Println(string(y))
	} else {
		j, _ := json.MarshalIndent(row, "", "  ")
		fmt.Println(string(j))
	}
}

func (o *Output) WriteList(header []string, tableRows []Row) {
	if o.Format == "table" {
		WriteTable(header, tableRows)
	} else if o.Format == "yaml" {
		y, _ := yaml.Marshal(tableRows)
		fmt.Println(string(y))
	} else {
		j, _ := json.MarshalIndent(tableRows, "", "  ")
		fmt.Println(string(j))
	}
}

type GenericRow struct {
	Data []string
}

func (r *GenericRow) DataAsStringArray() []string {
	return r.Data
}

type Row interface {
	DataAsStringArray() []string
}

func WriteTable(header []string, tableRows []Row) {
	table := tablewriter.NewWriter(os.Stdout)
	table.SetHeader(header)
	table.SetAutoWrapText(false)
	for _, v := range tableRows {
		table.Append(v.DataAsStringArray())
	}
	table.Render() // Send output
}
