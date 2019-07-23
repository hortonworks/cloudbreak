package oauth

import (
	"github.com/go-openapi/strfmt"
	environmentclient "github.com/hortonworks/cb-cli/dataplane/api-environment/client"
	freeipaclient "github.com/hortonworks/cb-cli/dataplane/api-freeipa/client"
	redbeamsclient "github.com/hortonworks/cb-cli/dataplane/api-redbeams/client"
	sdxclient "github.com/hortonworks/cb-cli/dataplane/api-sdx/client"
	apiclient "github.com/hortonworks/cb-cli/dataplane/api/client"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	u "github.com/hortonworks/cb-cli/dataplane/utils"
	"github.com/hortonworks/dp-cli-common/apikeyauth"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

var PREFIX_TRIM = []string{"http://", "https://"}

type Cloudbreak struct {
	Cloudbreak *apiclient.Cloudbreak
}
type Sdx struct {
	Sdx *sdxclient.Datalake
}
type FreeIpa struct {
	FreeIpa *freeipaclient.FreeIPA
}

type Environment struct {
	Environment *environmentclient.Environment
}

type Redbeams struct {
	Redbeams *redbeamsclient.Redbeams
}

func NewCloudbreakHTTPClientFromContext(c *cli.Context) *Cloudbreak {
	return NewCloudbreakHTTPClient(c.String(fl.FlServerOptional.Name), c.String(fl.FlApiKeyIDOptional.Name), c.String(fl.FlPrivateKeyOptional.Name))
}

func NewCloudbreakHTTPClient(address string, apiKeyID, privateKey string) *Cloudbreak {
	u.CheckServerAddress(address)
	var transport *utils.Transport
	baseAPIPath := "/cb/api"
	transport = apikeyauth.GetAPIKeyAuthTransport(address, baseAPIPath, apiKeyID, privateKey)
	return &Cloudbreak{Cloudbreak: apiclient.New(transport, strfmt.Default)}
}

func NewCloudbreakActorCrnHTTPClient(address string, actorCrn string) *Cloudbreak {
	u.CheckServerAddress(address)
	var transport *utils.Transport
	baseAPIPath := "/cb/api"
	transport = apikeyauth.GetActorCrnAuthTransport(address, baseAPIPath, actorCrn)
	return &Cloudbreak{Cloudbreak: apiclient.New(transport, strfmt.Default)}
}

// NewSDXHTTPClientFromContext : Initialize Sdx client.
func NewSDXClientFromContext(c *cli.Context) *Sdx {
	return NewSDXClient(c.String(fl.FlServerOptional.Name), c.String(fl.FlApiKeyIDOptional.Name), c.String(fl.FlPrivateKeyOptional.Name))
}

func NewSDXClient(address string, apiKeyID, privateKey string) *Sdx {
	u.CheckServerAddress(address)
	var transport *utils.Transport
	const baseAPIPath string = "/dl/api"
	transport = apikeyauth.GetAPIKeyAuthTransport(address, baseAPIPath, apiKeyID, privateKey)
	return &Sdx{Sdx: sdxclient.New(transport, strfmt.Default)}
}

func NewSDXActorCrnHTTPClient(address string, actorCrn string) *Sdx {
	u.CheckServerAddress(address)
	var transport *utils.Transport
	baseAPIPath := "/dl/api"
	transport = apikeyauth.GetActorCrnAuthTransport(address, baseAPIPath, actorCrn)
	return &Sdx{Sdx: sdxclient.New(transport, strfmt.Default)}
}

// NewDataplaneHTTPClientFromContext : Initialize FreeIpa client.
func NewFreeIpaClientFromContext(c *cli.Context) *FreeIpa {
	return NewFreeIpaClient(c.String(fl.FlServerOptional.Name), c.String(fl.FlApiKeyIDOptional.Name), c.String(fl.FlPrivateKeyOptional.Name))
}

func NewFreeIpaClient(address string, apiKeyID, privateKey string) *FreeIpa {
	u.CheckServerAddress(address)
	var transport *utils.Transport
	const baseAPIPath string = "/freeipa/api"
	transport = apikeyauth.GetAPIKeyAuthTransport(address, baseAPIPath, apiKeyID, privateKey)
	return &FreeIpa{FreeIpa: freeipaclient.New(transport, strfmt.Default)}
}

func NewFreeIpaActorCrnHTTPClient(address string, actorCrn string) *FreeIpa {
	u.CheckServerAddress(address)
	var transport *utils.Transport
	baseAPIPath := "/freeipa/api"
	transport = apikeyauth.GetActorCrnAuthTransport(address, baseAPIPath, actorCrn)
	return &FreeIpa{FreeIpa: freeipaclient.New(transport, strfmt.Default)}
}

// NewDataplaneHTTPClientFromContext : Initialize Environment client.
func NewEnvironmentClientFromContext(c *cli.Context) *Environment {
	return NewEnvironmentClient(c.String(fl.FlServerOptional.Name), c.String(fl.FlApiKeyIDOptional.Name), c.String(fl.FlPrivateKeyOptional.Name))
}

func NewEnvironmentClient(address string, apiKeyID, privateKey string) *Environment {
	u.CheckServerAddress(address)
	var transport *utils.Transport
	const baseAPIPath string = "/environmentservice/api"
	transport = apikeyauth.GetAPIKeyAuthTransport(address, baseAPIPath, apiKeyID, privateKey)
	return &Environment{Environment: environmentclient.New(transport, strfmt.Default)}
}

func NewRedbeamsClientFromContext(c *cli.Context) *Redbeams {
	return NewRedbeamsClientFrom(c.String(fl.FlServerOptional.Name), c.String(fl.FlApiKeyIDOptional.Name), c.String(fl.FlPrivateKeyOptional.Name))
}

func NewRedbeamsClientFrom(address string, apiKeyID, privateKey string) *Redbeams {
	u.CheckServerAddress(address)
	var transport *utils.Transport
	const baseAPIPath string = "/redbeams/api"
	transport = apikeyauth.GetAPIKeyAuthTransport(address, baseAPIPath, apiKeyID, privateKey)
	return &Redbeams{Redbeams: redbeamsclient.New(transport, strfmt.Default)}
}

func NewEnvironmentActorCrnHTTPClient(address string, actorCrn string) *Environment {
	u.CheckServerAddress(address)
	var transport *utils.Transport
	const baseAPIPath string = "/environmentservice/api"
	transport = apikeyauth.GetActorCrnAuthTransport(address, baseAPIPath, actorCrn)
	return &Environment{Environment: environmentclient.New(transport, strfmt.Default)}
}
