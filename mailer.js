var nodemailer = require('nodemailer');
var smtpTransport = require('nodemailer-smtp-transport');
var jade = require('jade');
var fs = require('fs');

exports.sendMail = function(to, subject, templateFile, data) {
    var transport = nodemailer.createTransport(smtpTransport({
        host: process.env.SL_SMTP_SENDER_HOST,
        port: process.env.SL_SMTP_SENDER_PORT,
        auth: {
            user: process.env.SL_SMTP_SENDER_USERNAME,
            pass: process.env.SL_SMTP_SENDER_PASSWORD
        }
    }));
    console.log('sending mail to ' +  to);
    var content = getEmailContent(templateFile, data)
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

getEmailContent = function(templateFile, data) {
    var content = fs.readFileSync(templateFile,'utf8')
    var fn = jade.compile(content); // TODO: check data
    return fn(data);
}