package ch.fhnw.ip6.powerconsumptionmanager.util.formatter;

import android.text.format.DateFormat;

import com.github.mikephil.charting.formatter.XAxisValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.Calendar;
import java.util.Locale;

/**
 * Created by Patrik on 09.07.2016.
 */
public class XAxisDateFormatter implements XAxisValueFormatter {

    @Override
    public String getXValue(String original, int index, ViewPortHandler viewPortHandler) {
        Calendar cal = Calendar.getInstance(Locale.GERMAN);
        cal.setTimeInMillis(Long.parseLong(original) * 1000);
        return DateFormat.format("dd.MM.yy", cal).toString();
    }
}
