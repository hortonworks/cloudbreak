package cli

var mockStringFinder = func(in string) (r string) {
	switch in {
	case FlCredentialName.Name:
		return "name"
	case FlDescription.Name:
		return "descritption"
	case FlRoleARN.Name:
		return "role-arn"
	default:
		return ""
	}
}

var mockBoolFinder = func(in string) (r bool) {
	return false
}
