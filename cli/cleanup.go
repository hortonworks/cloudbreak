package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/urfave/cli"
	"strings"
	"sync"
	"time"
)

func CleanupResources(c *cli.Context) error {
	defer timeTrack(time.Now(), "resource cleanup")

	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	var wg sync.WaitGroup
	wg.Add(5)

	go oAuth2Client.cleanupBlueprints(&wg)
	go oAuth2Client.cleanupTemplates(&wg)
	go oAuth2Client.cleanupCredentials(&wg)
	go oAuth2Client.cleanupNetworks(&wg)
	go oAuth2Client.cleanupSecurityGroups(&wg)

	wg.Wait()

	return nil
}

func (c *Cloudbreak) cleanupBlueprints(wg *sync.WaitGroup) {
	defer wg.Done()
	for _, bp := range c.GetPublicBlueprints() {
		if bp.Name[0] == 'b' && len(BlueprintMap[*bp.AmbariBlueprint.Blueprint.Name]) > 0 {
			if err := c.DeleteBlueprint(bp.Name); err != nil {
				log.Warnf("[cleanupBlueprints] failed to delete blueprint: %s", bp.Name)
				continue
			}
		}
	}
}

func (c *Cloudbreak) cleanupTemplates(wg *sync.WaitGroup) {
	defer wg.Done()
	for _, template := range c.GetPublicTemplates() {
		if strings.Contains(template.Name, "mtempl") || strings.Contains(template.Name, "wtempl") {
			if err := c.DeleteTemplate(template.Name); err != nil {
				log.Warnf("[cleanupTemplates] failed to delete template: %s", template.Name)
				continue
			}
		}
	}
}

func (c *Cloudbreak) cleanupCredentials(wg *sync.WaitGroup) {
	defer wg.Done()
	for _, cred := range c.GetPublicCredentials() {
		credName := cred.Name
		if len(credName) > 5 && credName[0:4] == "cred" {
			if err := c.DeleteCredential(credName); err != nil {
				log.Warnf("[cleanupCredentials] failed to delete credential: %s", credName)
				continue
			}
		}
	}
}

func (c *Cloudbreak) cleanupNetworks(wg *sync.WaitGroup) {
	defer wg.Done()
	for _, network := range c.GetPublicNetworks() {
		netName := network.Name
		if len(netName) > 4 && netName[0:3] == "net" {
			if err := c.DeleteNetwork(netName); err != nil {
				log.Warnf("[cleanupNetworks] failed to delete network: %s", netName)
				continue
			}
		}
	}
}

func (c *Cloudbreak) cleanupSecurityGroups(wg *sync.WaitGroup) {
	defer wg.Done()
	for _, secGroup := range c.GetPublicSecurityGroups() {
		secGroupName := secGroup.Name
		if len(secGroupName) > 5 && secGroupName[0:4] == "secg" {
			if err := c.DeleteSecurityGroup(secGroupName); err != nil {
				log.Warnf("[cleanupSecurityGroups] failed to delete security group: %s", secGroupName)
				continue
			}
		}
	}
}
