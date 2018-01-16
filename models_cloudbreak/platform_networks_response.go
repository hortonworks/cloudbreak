package models_cloudbreak

import (
	strfmt "github.com/go-openapi/strfmt"

	"github.com/go-openapi/errors"
	"github.com/go-openapi/swag"
	"github.com/go-openapi/validate"
)

type PlatformNetworksResponse struct {
	Networks map[string][]PlatformNetworkResponse `json:"networks,omitempty"`
}

func (m *PlatformNetworksResponse) Validate(formats strfmt.Registry) error {
	var res []error

	if err := m.validateNetworks(formats); err != nil {

		res = append(res, err)
	}

	if len(res) > 0 {
		return errors.CompositeValidationError(res...)
	}
	return nil
}

func (m *PlatformNetworksResponse) validateNetworks(formats strfmt.Registry) error {

	if swag.IsZero(m.Networks) {
		return nil
	}

	if err := validate.Required("networks", "body", m.Networks); err != nil {
		return err
	}

	for k := range m.Networks {

		if err := validate.Required("networks"+"."+k, "body", m.Networks[k]); err != nil {
			return err
		}

		if err := validate.UniqueItems("networks"+"."+k, "body", m.Networks[k]); err != nil {
			return err
		}

		for i := 0; i < len(m.Networks[k]); i++ {

			if swag.IsZero(m.Networks[k][i]) {
				continue
			}

		}

	}

	return nil
}

func (m *PlatformNetworksResponse) MarshalBinary() ([]byte, error) {
	if m == nil {
		return nil, nil
	}
	return swag.WriteJSON(m)
}

func (m *PlatformNetworksResponse) UnmarshalBinary(b []byte) error {
	var res PlatformNetworksResponse
	if err := swag.ReadJSON(b, &res); err != nil {
		return err
	}
	*m = res
	return nil
}
