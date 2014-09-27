var nodemailer = require('nodemailer');
var smtpTransport = require('nodemailer-smtp-transport');

exports.sendMail = function(to, subject, text) {
    console.log(process.env.UR_SMTP_SENDER_USERNAME)
    console.log(process.env.UR_SMTP_SENDER_PASSWORD)
    console.log(process.env.UR_SMTP_SENDER_FROM)
    console.log(process.env.UR_SMTP_SENDER_HOST)
    console.log(process.env.UR_SMTP_SENDER_PORT)
    var transport = nodemailer.createTransport(smtpTransport({
        host: process.env.UR_SMTP_SENDER_HOST,
        port: process.env.UR_SMTP_SENDER_PORT,
        auth: {
            user: process.env.UR_SMTP_SENDER_USERNAME,
            pass: process.env.UR_SMTP_SENDER_PASSWORD
        }
    }));
    console.log('sending mail to ' +  to);
    transport.sendMail({
        from: process.env.UR_SMTP_SENDER_FROM,
        to: to,
        subject: subject,
        text: text}, function(error, info){
        if(error){
            console.log(error);
        }else{
            console.log('Message sent: ' + info.response);
        }
    });
};