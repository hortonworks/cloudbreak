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

func (o *Output) WriteList(header []string, tableRows []Row) {
	if o.Format == "json" {
		j, _ := json.MarshalIndent(tableRows, "", "  ")
		fmt.Println(string(j))
	} else if o.Format == "yaml" {
		y, _ := yaml.Marshal(tableRows)
		fmt.Println(string(y))
	} else {
		WriteTable(header, tableRows)
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
