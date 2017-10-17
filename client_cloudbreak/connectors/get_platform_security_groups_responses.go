package connectors

import (
	"fmt"
	"io"

	"github.com/go-openapi/errors"
	"github.com/go-openapi/runtime"
	"github.com/go-openapi/validate"

	strfmt "github.com/go-openapi/strfmt"

	"github.com/hortonworks/cb-cli/models_cloudbreak"
)

type GetPlatformSecurityGroupsReader struct {
	formats strfmt.Registry
}

func (o *GetPlatformSecurityGroupsReader) ReadResponse(response runtime.ClientResponse, consumer runtime.Consumer) (interface{}, error) {
	switch response.Code() {

	case 200:
		result := NewGetPlatformSecurityGroupsOK()
		if err := result.readResponse(response, consumer, o.formats); err != nil {
			return nil, err
		}
		return result, nil

	default:
		return nil, runtime.NewAPIError("unknown error", response, response.Code())
	}
}

func NewGetPlatformSecurityGroupsOK() *GetPlatformSecurityGroupsOK {
	return &GetPlatformSecurityGroupsOK{}
}

type GetPlatformSecurityGroupsOK struct {
	Payload GetPlatformSecurityGroupsOKBody
}

func (o *GetPlatformSecurityGroupsOK) Error() string {
	return fmt.Sprintf("[POST /connectors/securitygroups][%d] getPlatformSecurityGroupsOK  %+v", 200, o.Payload)
}

func (o *GetPlatformSecurityGroupsOK) readResponse(response runtime.ClientResponse, consumer runtime.Consumer, formats strfmt.Registry) error {

	if err := consumer.Consume(response.Body(), &o.Payload); err != nil && err != io.EOF {
		return err
	}

	return nil
}

type GetPlatformSecurityGroupsOKBody map[string][]models_cloudbreak.PlatformSecurityGroupResponse

func (o GetPlatformSecurityGroupsOKBody) Validate(formats strfmt.Registry) error {
	var res []error

	if err := validate.Required("getPlatformSecurityGroupsOK", "body", o); err != nil {
		return err
	}

	for k := range o {

		if err := validate.Required("getPlatformSecurityGroupsOK"+"."+k, "body", o[k]); err != nil {
			return err
		}

		if err := validate.UniqueItems("getPlatformSecurityGroupsOK"+"."+k, "body", o[k]); err != nil {
			return err
		}

		for i := 0; i < len(o[k]); i++ {

		}

	}

	if len(res) > 0 {
		return errors.CompositeValidationError(res...)
	}
	return nil
}
