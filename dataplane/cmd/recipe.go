package cmd

import (
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/recipe"
	"github.com/urfave/cli"
)

func init() {
	DataPlaneCommands = append(DataPlaneCommands, cli.Command{
		Name:  "recipe",
		Usage: "recipe related operations",
		Subcommands: []cli.Command{
			{
				Name:  "create",
				Usage: "adds a new recipe from a file",
				Subcommands: []cli.Command{
					{
						Name:   "from-file",
						Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlExecutionType, fl.FlFile).AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: recipe.CreateRecipeFromFile,
						Usage:  "creates a recipe by reading it from a local file",
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlExecutionType, fl.FlFile).AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
			{
				Name:   "delete",
				Usage:  "deletes one or more recipes",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlNames).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: recipe.DeleteRecipes,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlNames).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "describe",
				Usage:  "describes a recipe",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: recipe.DescribeRecipe,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "list",
				Usage:  "lists the available recipes",
				Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: recipe.ListRecipes,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
		},
	})
}
