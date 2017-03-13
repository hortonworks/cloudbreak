package cli

import (
	"strings"
	"sync"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
	"github.com/urfave/cli"
)

func CleanupResources(c *cli.Context) error {
	defer timeTrack(time.Now(), "resource cleanup")

	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	var wg sync.WaitGroup
	wg.Add(6)

	go oAuth2Client.cleanupBlueprints(&wg)
	go oAuth2Client.cleanupTemplates(&wg)
	go oAuth2Client.cleanupCredentials(&wg)
	go oAuth2Client.cleanupNetworks(&wg)
	go oAuth2Client.cleanupSecurityGroups(&wg)
	go oAuth2Client.cleanupRecipes(&wg)

	wg.Wait()

	return nil
}

func (c *Cloudbreak) cleanupBlueprints(wg *sync.WaitGroup) {
	defer wg.Done()

	cleanupBlueprintsImpl(c.GetPublicBlueprints, c.DeleteBlueprint)
}

func cleanupBlueprintsImpl(getBlueprints func() []*models_cloudbreak.BlueprintResponse, deleteBlueprint func(string) error) {
	for _, bp := range getBlueprints() {
		if bp.Name[0] == 'b' && len(BlueprintMap[*bp.AmbariBlueprint.Blueprint.Name]) > 0 {
			if err := deleteBlueprint(bp.Name); err != nil {
				log.Warnf("[cleanupBlueprints] failed to delete blueprint: %s", bp.Name)
				continue
			}
		}
	}
}

func (c *Cloudbreak) cleanupTemplates(wg *sync.WaitGroup) {
	defer wg.Done()

	cleanupTemplatesImpl(c.GetPublicTemplates, c.DeleteTemplate)
}

func cleanupTemplatesImpl(getTemplates func() []*models_cloudbreak.TemplateResponse, deleteTemplate func(string) error) {
	for _, template := range getTemplates() {
		if strings.Contains(template.Name, "mtempl") || strings.Contains(template.Name, "wtempl") || strings.Contains(template.Name, "ctempl") {
			if err := deleteTemplate(template.Name); err != nil {
				log.Warnf("[cleanupTemplates] failed to delete template: %s", template.Name)
				continue
			}
		}
	}
}

func (c *Cloudbreak) cleanupCredentials(wg *sync.WaitGroup) {
	defer wg.Done()

	cleanupCredentialsImpl(c.GetPublicCredentials, c.DeleteCredential)
}

func cleanupCredentialsImpl(getCredentials func() []*models_cloudbreak.CredentialResponse, deleteCredential func(string) error) {
	for _, cred := range getCredentials() {
		credName := cred.Name
		if len(credName) > 5 && credName[0:4] == "cred" {
			if err := deleteCredential(credName); err != nil {
				log.Warnf("[cleanupCredentials] failed to delete credential: %s", credName)
				continue
			}
		}
	}
}

func (c *Cloudbreak) cleanupNetworks(wg *sync.WaitGroup) {
	defer wg.Done()

	cleanupNetworksImpl(c.GetPublicNetworks, c.DeleteNetwork)
}

func cleanupNetworksImpl(getNetworks func() []*models_cloudbreak.NetworkResponse, deleteNetwork func(string) error) {
	for _, network := range getNetworks() {
		netName := network.Name
		if len(netName) > 4 && netName[0:3] == "net" {
			if err := deleteNetwork(netName); err != nil {
				log.Warnf("[cleanupNetworks] failed to delete network: %s", netName)
				continue
			}
		}
	}
}

func (c *Cloudbreak) cleanupSecurityGroups(wg *sync.WaitGroup) {
	defer wg.Done()

	cleanupSecurityGroupsImpl(c.GetPublicSecurityGroups, c.DeleteSecurityGroup)
}

func cleanupSecurityGroupsImpl(getGroups func() []*models_cloudbreak.SecurityGroupResponse, deleteGroup func(string) error) {
	for _, secGroup := range getGroups() {
		secGroupName := secGroup.Name
		if len(secGroupName) > 6 && (secGroupName[0:7] == "hdc-sg-") {
			if err := deleteGroup(secGroupName); err != nil {
				log.Warnf("[cleanupSecurityGroups] failed to delete security group: %s", secGroupName)
				continue
			}
		}
	}
}

func (c *Cloudbreak) cleanupRecipes(wg *sync.WaitGroup) {
	defer wg.Done()

	cleanupRecipesImpl(c.GetPublicRecipes, c.DeleteRecipe)
}

func cleanupRecipesImpl(getRecipes func() []*models_cloudbreak.RecipeResponse, deleteRecipe func(string) error) {
	for _, recipe := range getRecipes() {
		recipeName := recipe.Name
		if strings.Contains(recipeName, "hrec") {
			if err := deleteRecipe(recipeName); err != nil {
				log.Warnf("[cleanupRecipesImpl] failed to delete recipe: %s", recipeName)
				continue
			}
		}
	}
}
