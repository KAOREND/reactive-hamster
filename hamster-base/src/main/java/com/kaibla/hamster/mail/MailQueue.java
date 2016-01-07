package com.kaibla.hamster.mail;

import com.kaibla.hamster.base.HamsterEngine;
import java.io.UnsupportedEncodingException;
import static java.lang.Thread.sleep;
import static java.net.URLDecoder.decode;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import static javax.mail.Session.getDefaultInstance;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailQueue implements Runnable {

    private final LinkedList queue = new LinkedList();
    private Thread th = null;
    HamsterEngine engine;
    private String mailServer = "localhost";
    String dftMime = "application/octet-stream";
    private String smtpPassword = "";
    private String smtpUser = "";
    private String sender = "";
    private String senderName = "";
    private boolean authenticate = true;
    private String testAddress = null;
    private String port="25";

    public MailQueue(HamsterEngine engine) {
        this.engine = engine;
        th = new Thread(this);
        th.start();
        engine.addThread(th);
    }

    public void sendMail(String adress, String subject,
            String message, boolean mirror, String mirrorMail) {
//        try {
        Mail mail = new Mail();
        mail.adress = adress;
        mail.subject = subject;
        mail.message = message;
        LOG.log(Level.INFO, "put mail in queue\n{0} \n{1}\n{2}", new Object[]{subject, adress, message});
        queue.add(mail);
        if (mirror) {
            mail = new Mail();
            mail.adress = mirrorMail;
            mail.subject = "[kaibla-mailmirror]"+ subject;
            mail.message = message;            
            queue.add(mail);
        }
        th.interrupt();
//        } catch (UnsupportedEncodingException ex) {
//            LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
//        }
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getPort() {
        return port;
    }
    
    

    /**
     * @return the mailServer
     */
    public String getMailServer() {
        return mailServer;
    }

    /**
     * @param mailServer the mailServer to set
     */
    public void setMailServer(String mailServer) {
        this.mailServer = mailServer;
    }

    /**
     * @return the smtpPassword
     */
    public String getSmtpPassword() {
        return smtpPassword;
    }

    /**
     * @param smtpPassword the smtpPassword to set
     */
    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    /**
     * @return the smtpUser
     */
    public String getSmtpUser() {
        return smtpUser;
    }

    /**
     * @param smtpUser the smtpUser to set
     */
    public void setSmtpUser(String smtpUser) {
        this.smtpUser = smtpUser;
    }

    class Mail {

        String adress;
        String subject;
        String message;
    }

    public void test() {
        Mail mail = new Mail();
        mail.subject = "test";
        mail.adress = "mailsanKai@web.de";
        mail.message = "Hallo";
//		sendMail(mail);
        queue.add(mail);
        th.interrupt();
    }

    private void sendMail(Mail mail) {
        int retCode = 0;

        try {
            // Prepare the properties
            Properties props = new Properties();
            props.put("mail.smtp.host", getMailServer());
            if(getPort() != null) {
                props.put("mail.smtp.port", getPort());
            }
            props.put("mail.debug", "true");
            if(getSmtpUser() != null) {
            props.setProperty("mail.user", getSmtpUser());
                 // Authentifizierung erzwingen
              props.setProperty("mail.smtp.auth", "" + authenticate);
            }
            
          
            // Create the mail
            MimeMessage msg = new MimeMessage(getDefaultInstance(props,
                    null));

            // Set sender
            msg.setFrom(new InternetAddress(sender, senderName,
                    "iso-8859-1"));

            if (testAddress != null) {
                mail.adress = testAddress;
            }
            // Set recipients
            // TO recipients
            Address[] address = {new InternetAddress(mail.adress, "",
                "iso-8859-1")};
            msg.setRecipients(MimeMessage.RecipientType.TO, address);

            // Set subject
            msg.setSubject(mail.subject);

            // Set content          
            msg.setContent(mail.message, "text/html; charset=utf-8");

            // Set sent date
            msg.setSentDate(new Date());

            Session session = getDefaultInstance(props);

            Transport transport = session.getTransport("smtp");
            transport.connect(getMailServer(), getSmtpUser(), getSmtpPassword());
            msg.saveChanges();
            transport.sendMessage(msg, msg.getAllRecipients());
            transport.close();

            LOG.info("send mail");
        } catch (MessagingException ex) {

            LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
            retCode = -10;

        } catch (UnsupportedEncodingException ex) {

            LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
            retCode = -20;

        }
    }

    public void setTestAddress(String testAddress) {
        this.testAddress = testAddress;
    }

    private synchronized Mail getNextMail() {
        return (Mail) queue.removeFirst();
    }

    @Override
    public void run() {
        while (!engine.isDestroyed()) {
            synchronized (this) {
                try {

                    while (!queue.isEmpty()) {
                        Mail mail = getNextMail();
                        sendMail(mail);
                    }

                } catch (Throwable thr) {
                    thr.printStackTrace();
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
        }
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setAuthenticate(boolean authenticate) {
        this.authenticate = authenticate;
    }
    private static final Logger LOG = getLogger(MailQueue.class
            .getName());

}
