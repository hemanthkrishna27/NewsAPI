package org.news.api.rss;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Main {

    public static void main(String[] args) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM"); // Full month name
        String monthName = sdf.format(cal.getTime());

        sdf = new SimpleDateFormat("YYYY");
        String yearName = sdf.format(cal.getTime());


        System.out.println(monthName+" "+yearName);
    }
}
