package models_cloudbreak

import (
	strfmt "github.com/go-openapi/strfmt"

	"github.com/go-openapi/errors"
	"github.com/go-openapi/swag"
	"github.com/go-openapi/validate"
)

type PlatformSecurityGroupsResponse struct {
	SecurityGroups map[string][]PlatformSecurityGroupResponse `json:"securityGroups,omitempty"`
}

func (m *PlatformSecurityGroupsResponse) Validate(formats strfmt.Registry) error {
	var res []error

	if err := m.validateSecurityGroups(formats); err != nil {

		res = append(res, err)
	}

	if len(res) > 0 {
		return errors.CompositeValidationError(res...)
	}
	return nil
}

func (m *PlatformSecurityGroupsResponse) validateSecurityGroups(formats strfmt.Registry) error {

	if swag.IsZero(m.SecurityGroups) {
		return nil
	}

	if err := validate.Required("securityGroups", "body", m.SecurityGroups); err != nil {
		return err
	}

	for k := range m.SecurityGroups {

		if err := validate.Required("securityGroups"+"."+k, "body", m.SecurityGroups[k]); err != nil {
			return err
		}

		if err := validate.UniqueItems("securityGroups"+"."+k, "body", m.SecurityGroups[k]); err != nil {
			return err
		}

		for i := 0; i < len(m.SecurityGroups[k]); i++ {

			if swag.IsZero(m.SecurityGroups[k][i]) {
				continue
			}

		}

	}

	return nil
}

func (m *PlatformSecurityGroupsResponse) MarshalBinary() ([]byte, error) {
	if m == nil {
		return nil, nil
	}
	return swag.WriteJSON(m)
}

func (m *PlatformSecurityGroupsResponse) UnmarshalBinary(b []byte) error {
	var res PlatformSecurityGroupsResponse
	if err := swag.ReadJSON(b, &res); err != nil {
		return err
	}
	*m = res
	return nil
}
