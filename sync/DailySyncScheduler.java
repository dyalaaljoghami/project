package sync;

import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class DailySyncScheduler {
    public static void main(String[] args) {
        Timer timer = new Timer();
        timer.schedule(new SyncTask(), getFirstRunTime(), 24 * 60 * 60 * 1000); // كل يوم
    }

    private static Date getFirstRunTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23); // مثلاً الساعة 11 مساءً
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 0);
        if (calendar.getTime().before(new Date())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return calendar.getTime();
    }
}
