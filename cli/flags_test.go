package cli

var mockStringFinder = func(in string) (r string) {
	switch in {
	case FlName.Name:
		return "name"
	case FlDescriptionOptional.Name:
		return "descritption"
	default:
		return ""
	}
}

var mockBoolFinder = func(in string) (r bool) {
	return false
}
