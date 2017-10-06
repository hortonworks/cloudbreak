package connectors

import (
	"fmt"
	"io"

	"github.com/go-openapi/errors"
	"github.com/go-openapi/runtime"
	"github.com/go-openapi/swag"
	"github.com/go-openapi/validate"

	strfmt "github.com/go-openapi/strfmt"

	"github.com/hortonworks/hdc-cli/models_cloudbreak"
)

type GetPlatformSShKeysReader struct {
	formats strfmt.Registry
}

func (o *GetPlatformSShKeysReader) ReadResponse(response runtime.ClientResponse, consumer runtime.Consumer) (interface{}, error) {
	switch response.Code() {

	case 200:
		result := NewGetPlatformSShKeysOK()
		if err := result.readResponse(response, consumer, o.formats); err != nil {
			return nil, err
		}
		return result, nil

	default:
		return nil, runtime.NewAPIError("unknown error", response, response.Code())
	}
}

func NewGetPlatformSShKeysOK() *GetPlatformSShKeysOK {
	return &GetPlatformSShKeysOK{}
}

type GetPlatformSShKeysOK struct {
	Payload GetPlatformSShKeysOKBody
}

func (o *GetPlatformSShKeysOK) Error() string {
	return fmt.Sprintf("[POST /connectors/sshkeys][%d] getPlatformSShKeysOK  %+v", 200, o.Payload)
}

func (o *GetPlatformSShKeysOK) readResponse(response runtime.ClientResponse, consumer runtime.Consumer, formats strfmt.Registry) error {

	if err := consumer.Consume(response.Body(), &o.Payload); err != nil && err != io.EOF {
		return err
	}

	return nil
}

type GetPlatformSShKeysOKBody map[string][]models_cloudbreak.PlatformSSHKeyResponse

func (o GetPlatformSShKeysOKBody) Validate(formats strfmt.Registry) error {
	var res []error

	if err := validate.Required("getPlatformSShKeysOK", "body", o); err != nil {
		return err
	}

	for k := range o {

		if err := validate.Required("getPlatformSShKeysOK"+"."+k, "body", o[k]); err != nil {
			return err
		}

		if err := validate.UniqueItems("getPlatformSShKeysOK"+"."+k, "body", o[k]); err != nil {
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
