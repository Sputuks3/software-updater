package updater.downloader;

import updater.util.CommonUtil;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public class Util extends CommonUtil {

    protected Util() {
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        // http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
        int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static String humanReadableTimeCount(int timeInSecond, int maxDisplay) {
        int buf = timeInSecond, count = 0;
        StringBuilder sb = new StringBuilder();

        if (buf >= 31536000) {
            int year = buf / 31536000;
            buf %= 31536000;

            sb.append(year);
            sb.append(" yr");
            sb.append(year > 1 ? 's' : "");

            count++;
        }
        if (count < maxDisplay && (buf >= 2592000 || count != 0)) {
            sb.append(count != 0 ? ", " : "");

            int month = buf / 2592000;
            buf %= 2592000;

            sb.append(month);
            sb.append(" mth");
            sb.append(month > 1 ? 's' : "");

            count++;
        }
        if (count < maxDisplay && (buf >= 86400 || count != 0)) {
            sb.append(count != 0 ? ", " : "");

            int day = buf / 86400;
            buf %= 86400;

            sb.append(day);
            sb.append(" day");
            sb.append(day > 1 ? 's' : "");

            count++;
        }
        if (count < maxDisplay && (buf >= 3600 || count != 0)) {
            sb.append(count != 0 ? ", " : "");

            int hour = buf / 3600;
            buf %= 3600;

            sb.append(hour);
            sb.append('h');

            count++;
        }
        if (count < maxDisplay && (buf >= 60 || count != 0)) {
            sb.append(count != 0 ? ' ' : "");

            int minute = buf / 60;
            buf %= 60;

            sb.append(minute);
            sb.append('m');

            count++;
        }
        if (count < maxDisplay) {
            sb.append(count != 0 ? ' ' : "");

            sb.append(buf);
            sb.append('s');
        }

        return sb.toString();
    }
}