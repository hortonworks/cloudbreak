package model

import (
	"github.com/go-openapi/strfmt"

	"github.com/go-openapi/errors"
	"github.com/go-openapi/swag"
	"github.com/go-openapi/validate"
)

type PlatformGatewaysResponse struct {
	Gateways map[string][]CloudGatewayJSON `json:"gateways,omitempty"`
}

func (m *PlatformGatewaysResponse) Validate(formats strfmt.Registry) error {
	var res []error

	if err := m.validateGateways(formats); err != nil {

		res = append(res, err)
	}

	if len(res) > 0 {
		return errors.CompositeValidationError(res...)
	}
	return nil
}

func (m *PlatformGatewaysResponse) validateGateways(formats strfmt.Registry) error {

	if swag.IsZero(m.Gateways) {
		return nil
	}

	if err := validate.Required("gateways", "body", m.Gateways); err != nil {
		return err
	}

	for k := range m.Gateways {

		if err := validate.Required("gateways"+"."+k, "body", m.Gateways[k]); err != nil {
			return err
		}

		if err := validate.UniqueItems("gateways"+"."+k, "body", m.Gateways[k]); err != nil {
			return err
		}

		for i := 0; i < len(m.Gateways[k]); i++ {

			if swag.IsZero(m.Gateways[k][i]) {
				continue
			}

		}

	}

	return nil
}

func (m *PlatformGatewaysResponse) MarshalBinary() ([]byte, error) {
	if m == nil {
		return nil, nil
	}
	return swag.WriteJSON(m)
}

func (m *PlatformGatewaysResponse) UnmarshalBinary(b []byte) error {
	var res PlatformGatewaysResponse
	if err := swag.ReadJSON(b, &res); err != nil {
		return err
	}
	*m = res
	return nil
}
