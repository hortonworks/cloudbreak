package v1connectors

import (
	"fmt"
	"io"

	"github.com/go-openapi/errors"
	"github.com/go-openapi/runtime"
	"github.com/go-openapi/swag"
	"github.com/go-openapi/validate"

	strfmt "github.com/go-openapi/strfmt"

	"github.com/hortonworks/cb-cli/models_cloudbreak"
)

type GetPlatformNetworksReader struct {
	formats strfmt.Registry
}

func (o *GetPlatformNetworksReader) ReadResponse(response runtime.ClientResponse, consumer runtime.Consumer) (interface{}, error) {
	switch response.Code() {

	case 200:
		result := NewGetPlatformNetworksOK()
		if err := result.readResponse(response, consumer, o.formats); err != nil {
			return nil, err
		}
		return result, nil

	default:
		return nil, runtime.NewAPIError("unknown error", response, response.Code())
	}
}

func NewGetPlatformNetworksOK() *GetPlatformNetworksOK {
	return &GetPlatformNetworksOK{}
}

type GetPlatformNetworksOK struct {
	Payload GetPlatformNetworksOKBody
}

func (o *GetPlatformNetworksOK) Error() string {
	return fmt.Sprintf("[POST /v1/connectors/networks][%d] getPlatformNetworksOK  %+v", 200, o.Payload)
}

func (o *GetPlatformNetworksOK) readResponse(response runtime.ClientResponse, consumer runtime.Consumer, formats strfmt.Registry) error {

	if err := consumer.Consume(response.Body(), &o.Payload); err != nil && err != io.EOF {
		return err
	}

	return nil
}

type GetPlatformNetworksOKBody map[string][]models_cloudbreak.PlatformNetworkResponse

func (o GetPlatformNetworksOKBody) Validate(formats strfmt.Registry) error {
	var res []error

	if err := validate.Required("getPlatformNetworksOK", "body", o); err != nil {
		return err
	}

	for k := range o {

		if err := validate.Required("getPlatformNetworksOK"+"."+k, "body", o[k]); err != nil {
			return err
		}

		if err := validate.UniqueItems("getPlatformNetworksOK"+"."+k, "body", o[k]); err != nil {
			return err
		}

		for i := 0; i < len(o[k]); i++ {

			if swag.IsZero(o[k][i]) {
				continue
			}

		}

	}

	if len(res) > 0 {
		return errors.CompositeValidationError(res...)
	}
	return nil
}
