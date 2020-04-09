package it.elcasa.sqlreport.samples

import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class TestSendEmail {

    static void main(String[] args) {

        final String username = "username@gmail.com"
        // if you have 2 step verification, generate a specific app password
        final String password = "password"

        Properties prop = new Properties()
        prop.put("mail.smtp.host", "smtp.gmail.com")
        prop.put("mail.smtp.port", "587")
        prop.put("mail.smtp.auth", "true")
        prop.put("mail.smtp.starttls.enable", "true") //TLS

        javax.mail.Authenticator auth = null
        if (prop['mail.smtp.auth']?.equals('true')){
            auth = new javax.mail.Authenticator() {
                protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new javax.mail.PasswordAuthentication(username, password)
                }
            }
        }
        Session session = Session.getInstance(prop, auth)

        try {
            Message message = new MimeMessage(session)
            message.setFrom(new InternetAddress("from@gmail.com"))
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse("test@email.com, to_username_b@yahoo.com")
            )
            message.setSubject("Testing Gmail TLS")
            message.setText("Dear Mail Crawler,"
                    + "\n\n Please do not spam my email!")

            Transport.send(message)

            System.out.println("Done")

        } catch (MessagingException e) {
            e.printStackTrace()
        }
    }

}
