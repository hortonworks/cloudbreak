package model

import (
	"github.com/go-openapi/strfmt"

	"github.com/go-openapi/errors"
	"github.com/go-openapi/swag"
	"github.com/go-openapi/validate"
)

type PlatformSSHKeysResponse struct {
	SSHKeys map[string][]PlatformSSHKeyResponse `json:"sshKeys,omitempty"`
}

func (m *PlatformSSHKeysResponse) Validate(formats strfmt.Registry) error {
	var res []error

	if err := m.validateSSHKeys(formats); err != nil {

		res = append(res, err)
	}

	if len(res) > 0 {
		return errors.CompositeValidationError(res...)
	}
	return nil
}

func (m *PlatformSSHKeysResponse) validateSSHKeys(formats strfmt.Registry) error {

	if swag.IsZero(m.SSHKeys) {
		return nil
	}

	if err := validate.Required("sshKeys", "body", m.SSHKeys); err != nil {
		return err
	}

	for k := range m.SSHKeys {

		if err := validate.Required("sshKeys"+"."+k, "body", m.SSHKeys[k]); err != nil {
			return err
		}

		if err := validate.UniqueItems("sshKeys"+"."+k, "body", m.SSHKeys[k]); err != nil {
			return err
		}

		for i := 0; i < len(m.SSHKeys[k]); i++ {

			if swag.IsZero(m.SSHKeys[k][i]) {
				continue
			}

		}

	}

	return nil
}

func (m *PlatformSSHKeysResponse) MarshalBinary() ([]byte, error) {
	if m == nil {
		return nil, nil
	}
	return swag.WriteJSON(m)
}

func (m *PlatformSSHKeysResponse) UnmarshalBinary(b []byte) error {
	var res PlatformSSHKeysResponse
	if err := swag.ReadJSON(b, &res); err != nil {
		return err
	}
	*m = res
	return nil
}
