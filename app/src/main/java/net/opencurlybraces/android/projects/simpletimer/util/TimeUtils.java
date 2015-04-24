package net.opencurlybraces.android.projects.simpletimer.util;

import java.util.Formatter;
import java.util.Locale;

/**
 * Created by chris on 23/04/15.
 */
public class TimeUtils {

    private static final String TAG = "TimeUtils";

    private static final String sElapsedFormatHMMSSSS = "%1$d:%2$02d:%3$02d:%4$02d";
    private static final String sElapsedFormatMMSSSS = "%1$02d:%2$02d:%3$02d";

    private static final int SECOND_IN_MILLIS = 1000;

    private TimeUtils() {
    } ;

    /**
     * Formats an elapsed time in a format like "MM:SS:SS" or "H:MM:SS:SS" (using a form
     * suited to the current locale), similar to that used on the call-in-progress
     * screen.
     *
     * @param recycle {@link StringBuilder} to recycle, or null to use a temporary one.
     * @param elapsedMillis the elapsed time in milliseconds.
     */
    public static String formatElapsedTime(StringBuilder recycle, long elapsedMillis) {
        return TimeUtils.formatMillis(recycle, elapsedMillis);
    }

    private static String formatMillis(StringBuilder recycle,
                                       long elapsedMillis) {
        long hours = 0;
        long minutes = 0;
        long seconds = 0;

        if (elapsedMillis >= 3600 * SECOND_IN_MILLIS) {
            hours = elapsedMillis / (3600 * SECOND_IN_MILLIS);
            elapsedMillis -= hours * 3600 * SECOND_IN_MILLIS;
        }

        if (elapsedMillis >= 60 * SECOND_IN_MILLIS) {
            minutes = elapsedMillis / (60 * SECOND_IN_MILLIS);
            elapsedMillis -= minutes * 60 * SECOND_IN_MILLIS;
        }

        if (elapsedMillis >= SECOND_IN_MILLIS) {
            seconds = (elapsedMillis / SECOND_IN_MILLIS);
            elapsedMillis -= (seconds * SECOND_IN_MILLIS);
        }

        StringBuilder sb = recycle;
        if (sb == null) {
            sb = new StringBuilder(8);
        } else {
            sb.setLength(0);
        }

        Formatter f = new Formatter(sb, Locale.getDefault());
        if (hours > 0) {
            return f.format(sElapsedFormatHMMSSSS, hours, minutes, seconds,
                    elapsedMillis / 10).toString();
        } else {
            return f.format(sElapsedFormatMMSSSS, minutes, seconds, elapsedMillis / 10).toString();
        }
    }

}
