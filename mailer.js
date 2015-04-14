var nodemailer = require('nodemailer');
var smtpTransport = require('nodemailer-smtp-transport');
var jade = require('jade');
var fs = require('fs');

exports.sendMail = function(to, subject, templateFile, data) {
    var content = getEmailContent(templateFile, data)
    console.log('sending email. content: ' + content)
    if (process.env.SL_SMTP_SENDER_HOST != null && process.env.SL_SMTP_SENDER_FROM != null) {
        sendSimpleEmail(to, subject, content);
    } else {
        sendDummyEmail();
    }
};

sendSimpleEmail = function(to, subject, content) {
    var transport = nodemailer.createTransport(smtpTransport({
        host: process.env.SL_SMTP_SENDER_HOST,
        port: process.env.SL_SMTP_SENDER_PORT,
        auth: {
            user: process.env.SL_SMTP_SENDER_USERNAME,
            pass: process.env.SL_SMTP_SENDER_PASSWORD
        }
    }));
    console.log('sending mail to ' +  to);
    transport.sendMail({
        from: process.env.SL_SMTP_SENDER_FROM,
        to: to,
        subject: subject,
        html: content}, function(error, info){
        if(error){
            console.log(error);
        }else{
            console.log('Message sent: ' + info.response);
        }
    });
};

sendDummyEmail = function() {
    console.log("SMTP not configured! Related configuration entries: " + missingVars());
};

missingVars = function() {
    var vars = []
    if (process.env.SL_SMTP_SENDER_HOST == null) {
        vars.push("SL_SMTP_SENDER_HOST")
    }
    if (process.env.SL_SMTP_SENDER_PORT == null) {
        vars.push("SL_SMTP_SENDER_PORT")
    }
    if (process.env.SL_SMTP_SENDER_USERNAME == null) {
        vars.push("SL_SMTP_SENDER_USERNAME")
    }
    if (process.env.SL_SMTP_SENDER_PASSWORD == null) {
        vars.push("SL_SMTP_SENDER_PASSWORD")
    }
    if (process.env.SL_SMTP_SENDER_FROM == null) {
        vars.push("SL_SMTP_SENDER_FROM")
    }
    return vars.toString();
}

getEmailContent = function(templateFile, data) {
    var content = fs.readFileSync(templateFile,'utf8')
    var fn = jade.compile(content); // TODO: check data
    return fn(data);
}