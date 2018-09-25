package model

import (
	strfmt "github.com/go-openapi/strfmt"

	"github.com/go-openapi/errors"
	"github.com/go-openapi/swag"
	"github.com/go-openapi/validate"
)

type PlatformIPPoolsResponse struct {
	Ippools map[string][]IPPoolJSON `json:"ippools,omitempty"`
}

func (m *PlatformIPPoolsResponse) Validate(formats strfmt.Registry) error {
	var res []error

	if err := m.validateIppools(formats); err != nil {

		res = append(res, err)
	}

	if len(res) > 0 {
		return errors.CompositeValidationError(res...)
	}
	return nil
}

func (m *PlatformIPPoolsResponse) validateIppools(formats strfmt.Registry) error {

	if swag.IsZero(m.Ippools) {
		return nil
	}

	if err := validate.Required("ippools", "body", m.Ippools); err != nil {
		return err
	}

	for k := range m.Ippools {

		if err := validate.Required("ippools"+"."+k, "body", m.Ippools[k]); err != nil {
			return err
		}

		if err := validate.UniqueItems("ippools"+"."+k, "body", m.Ippools[k]); err != nil {
			return err
		}

		for i := 0; i < len(m.Ippools[k]); i++ {

			if swag.IsZero(m.Ippools[k][i]) {
				continue
			}

		}

	}

	return nil
}

func (m *PlatformIPPoolsResponse) MarshalBinary() ([]byte, error) {
	if m == nil {
		return nil, nil
	}
	return swag.WriteJSON(m)
}

func (m *PlatformIPPoolsResponse) UnmarshalBinary(b []byte) error {
	var res PlatformIPPoolsResponse
	if err := swag.ReadJSON(b, &res); err != nil {
		return err
	}
	*m = res
	return nil
}
