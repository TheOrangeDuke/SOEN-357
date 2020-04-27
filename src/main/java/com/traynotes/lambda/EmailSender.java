package com.traynotes.lambda;

import net.sargue.mailgun.Configuration;
import net.sargue.mailgun.Mail;
import net.sargue.mailgun.Response;

public class EmailSender {

    // This address must be verified with Amazon SES.
    static final String FROM = "ed.paraschivescu@gmail.com";

    // The subject line for the email.
    static final String SUBJECT = "Lost Paws Mtl - Your Link";

    // The HTML body for the email.
    static final String HTMLBODY_START = "<h1>Lost Paws Mtl</h1>"
            + "<p>Your ad is now ready to be enabled. Click on the following link to manage it.</p>"
            + "<p><a href='";

    static final String HTMLBODY_END = "'>Your ad</a></p>";

    public static void sendConfirmationEmail(String recipient, String advertismentUrl){
        System.out.println("Sending email ! " + recipient + " will receive " + advertismentUrl);
        try {
            Configuration configuration = new Configuration()
                    .domain("https://api.mailgun.net/v3/lostpawsmtl.com/messages")
                    .apiKey("c57edb536a2a8e98cd090fab043287a8-f135b0f1-c279bd9c")
                    .from("Mailgun Sandbox", "postmaster@lostpawsmtl.com");
            Response r = Mail.using(configuration)
                    .to(recipient)
                    .subject(SUBJECT)
                    .text(HTMLBODY_START + advertismentUrl + HTMLBODY_END)
                    .build()
                    .send();
            System.out.println("Email sent! (according to system) -> is ok:" + r.isOk() + " code:" + r.responseCode() + " message:" + r.responseMessage());
        } catch (Exception ex) {
            System.out.println("The email was not sent. Error message: "
                    + ex.getMessage());
        }
    }
}
