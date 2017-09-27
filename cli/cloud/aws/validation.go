package aws

import (
	"errors"
	"fmt"
	"regexp"
	"strings"

	"github.com/go-openapi/validate"
	"github.com/hortonworks/hdc-cli/cli/cloud"
)

func (p *AwsProvider) ValidateNetwork(n *cloud.Network) []error {
	var res []error = nil
	if len(n.VpcId) != 0 || len(n.SubnetId) != 0 {
		if err := validate.RequiredString("VpcId", "network", n.VpcId); err != nil {
			res = append(res, err)
		}
		if err := validate.RequiredString("SubnetId", "network", n.SubnetId); err != nil {
			res = append(res, err)
		}
	}
	return res
}

func (p *AwsProvider) ValidateTags(tags map[string]string) []error {
	var res []error = make([]error, 0)

	pattern := `^[a-zA-Z0-9+ \-=.@_:/]*$`

	if len(tags) > 10 {
		res = append(res, errors.New("Maximum number of tags allowed: 10"))
	}

	for k, v := range tags {
		if strings.HasPrefix(k, "aws") || strings.HasPrefix(v, "aws") {
			res = append(res, errors.New("'aws' is a reserved prefix, cannot start the key or value with it"))
		}
		if len(k) > 127 {
			res = append(res, errors.New("Key length cannot be longer than 127 chars, key: "+k))
		}
		if len(v) > 255 {
			res = append(res, errors.New("Value length cannot be longer than 255 chars, value: "+v))
		}
		if match, _ := regexp.Match(pattern, []byte(k)); !match {
			res = append(res, errors.New(fmt.Sprintf("The key (%s) contains invalid characters. "+
				"Allowed characters are letters, whitespace, and numbers representable in UTF-8, plus the following special characters: + - = . _ : /", k)))
		}
		if match, _ := regexp.Match(pattern, []byte(v)); !match {
			res = append(res, errors.New(fmt.Sprintf("The value (%s) contains invalid characters. "+
				"Allowed characters are letters, whitespace, and numbers representable in UTF-8, plus the following special characters: + - = . _ : /", v)))
		}
	}
	return res
}
