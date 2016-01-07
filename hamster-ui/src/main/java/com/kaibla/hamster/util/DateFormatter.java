/**
 *
 */
package com.kaibla.hamster.util;

import com.kaibla.hamster.base.HamsterPage;
import static com.kaibla.hamster.util.HTMLCodeFilter.getStrictFilteredString;
import java.sql.Timestamp;
import java.text.DateFormat;
import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getTimeInstance;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 * @author Jakob Kummerow
 */
public class DateFormatter {

    public static String date(Timestamp time, HamsterPage page) {
        if (time == null) {
            return "";
        }
        Locale l = Locale.ENGLISH;
        DateFormat df = getDateInstance(DateFormat.MEDIUM, l);
        Timestamp now = new Timestamp(new Date().getTime());
        String today = df.format(now);
        String datestring = df.format(time);
        if (today.equals(datestring)) {
            return "<strong>Heute</strong>"; // ! TODO: i18n
        }
        return getStrictFilteredString(datestring);
    }

    public static String fullDate(Timestamp time, HamsterPage page) {
        if (time == null) {
            return "";
        }
        Locale l = Locale.ENGLISH;
        DateFormat df = getDateInstance(DateFormat.FULL, l);
        Timestamp now = new Timestamp(new Date().getTime());
        String today = df.format(now);
        String datestring = df.format(time);
        if (today.equals(datestring)) {
            return "Heute"; // ! TODO: i18n
        }
        return getStrictFilteredString(datestring);
    }

    public static String datetime(Timestamp time, HamsterPage page) {
        if (time == null) {
            return "";
        }
        Locale l = Locale.ENGLISH;
        DateFormat df = getTimeInstance(DateFormat.SHORT, l);
        return date(time, page) + ", " + getStrictFilteredString(df.format(time));
    }

    public static String getTimeOnly(Timestamp time, HamsterPage page) {
        if (time == null) {
            return "";
        }
        Locale l = Locale.ENGLISH;
        DateFormat df = getTimeInstance(DateFormat.SHORT, l);
        return getStrictFilteredString(df.format(time));
    }

//    public static String date(Timestamp timestamp) {
//	return null;
//    }
    private static final Logger LOG = getLogger(DateFormatter.class.getName());

}
