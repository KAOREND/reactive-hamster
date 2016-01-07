/*
 * 
 * .
 */
package com.kaibla.hamster.mail;

import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 *
 * @author kai
 */
public class MailAddress extends InternetAddress {

    public MailAddress() {
        super();
    }

    public MailAddress(String arg0) throws AddressException {
        super(arg0);
    }

    public MailAddress(String arg0, boolean arg1) throws AddressException {
        super(arg0, arg1);
    }

    public MailAddress(String arg0, String arg1) throws UnsupportedEncodingException {
        super(arg0, arg1);
    }

    public MailAddress(String arg0, String arg1, String arg2) throws UnsupportedEncodingException {
        super(arg0, arg1, arg2);
    }

    public String getName() {
        String name = getAddress().substring(0, getAddress().indexOf('@'));
        return name;
    }
    private static final Logger LOG = getLogger(MailAddress.class.getName());

}
