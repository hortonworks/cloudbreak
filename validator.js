var validator = require('validator');

exports.validateRegister = function(email, password, firstName, lastName, company) {
    var result = null
    if (checkEmail(email, validator)) {
        result = 'email field is invalid'
    } else if (validator.isNull(password) || !validator.isLength(password, 6)) {
        result = 'password field is invalid. (min length.: 6)'
    } else if (validator.isNull(firstName) || !validator.isLength(firstName, 1)) { // change length if needed
        result = 'first name field is invalid.'
    } else if (validator.isNull(lastName) || !validator.isLength(lastName, 1)) {
        result = 'last name field is invalid.'
    } else if (validator.isNull(company) || !validator.isLength(company, 1)) {
        result = 'company field is invalid.'
    }
    return result;
};

exports.validateForget = function(email) {
    var result = null;
    if (checkEmail(email, validator)) {
        result = 'email field is invalid'
    }
    return result;
};

exports.validateReset = function(email, password) {
    var result = null;
    if (checkEmail(email, validator)) {
        result = 'email field is invalid'
    } else if (validator.isNull(password) || !validator.isLength(password, 6)) {
        result = 'password field is invalid. (min length.: 6)'
    }
    return result;
};

exports.validateEmail = function(email) {
    return checkEmail(email, validator)
};

checkEmail = function(email, validator) {
    return (validator.isNull(email) || !validator.isEmail(email) || !validator.isLowercase(email))
}