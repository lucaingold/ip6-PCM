package ch.fhnw.ip5.powerconsumptionmanager.network;

import android.os.AsyncTask;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import ch.fhnw.ip5.powerconsumptionmanager.R;
import ch.fhnw.ip5.powerconsumptionmanager.model.PlanEntryModel;
import ch.fhnw.ip5.powerconsumptionmanager.model.RouteInformationModel;
import ch.fhnw.ip5.powerconsumptionmanager.util.CalendarInstanceReader;
import ch.fhnw.ip5.powerconsumptionmanager.util.PowerConsumptionManagerAppContext;

/**
 * Background task to build the JSON of the charge plan data that needs to be synced
 */
public class PlanSyncStringBuilderTask extends AsyncTask<Void, Void, String> {
    private PowerConsumptionManagerAppContext mContext;
    private StringBuilder mData;

    public PlanSyncStringBuilderTask(PowerConsumptionManagerAppContext context) {
        mContext = context;
        mData = new StringBuilder(1000);
    }

    @Override
    protected String doInBackground(Void... params) {
        boolean error = false;
        Calendar calendar = Calendar.getInstance();
        CalendarInstanceReader cir = new CalendarInstanceReader(calendar, mContext.getApplicationContext());

        // Calculate the lower and upper range to request tesla trips to sync (one week range)
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), 1, 0, 0, 0);
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        calendar.set(Calendar.MILLISECOND, 0);
        long lowerRangeEnd = calendar.getTimeInMillis();

        /*
         * Get all the days that are being synched (needed because the keys in the hashmap are the
         * days where the calendar instance takes place)
         */
        int[] dayKeys = new int[7];
        for(int i = 0; i < 7; i++) {
            dayKeys[i] = calendar.get(Calendar.DAY_OF_MONTH);
            if(i != 6) {
                calendar.add(Calendar.DAY_OF_WEEK, 1);
            }
        }
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        long upperRangeEnd = calendar.getTimeInMillis();

        // Get all calendar instances that take place in the current week
        HashMap<Integer, PlanEntryModel> instances = cir.readInstancesBetweenTimestamps(lowerRangeEnd, upperRangeEnd);

        String day;
        String weekday;
        String start;
        String end;
        int kilometer;

        // Build the actual JSON that is being sent
        mData.append("[");
        for (int i = 0; i < 7; i++) {
            // Check if a calendar instance exists to a day that needs to be synched
            if (instances.containsKey(dayKeys[i])) {
                PlanEntryModel pem = instances.get(dayKeys[i]);
                String[] locations = pem.getEventLocation().split("/");
                // Check if an event location has been specified
                if (locations.length == 2 && !"".equals(locations[0]) && !"".equals(locations[1])) {
                    Request request = new Request.Builder()
                            .url(mContext.getString(R.string.googleMaps_Api1) +
                                    "origin=" + locations[0] +
                                    "&destination=" + locations[1] +
                                    mContext.getString(R.string.googleMaps_Api2))
                            .build();

                    OkHttpClient client = new OkHttpClient();
                    try {
                        Response response = client.newCall(request).execute();
                        DataLoader loader = new DataLoader(mContext, null);
                        if (!loader.processRoutes(response)) {
                            error = true;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        error = true;
                    }
                }

                // Format the information that need to be sent
                SimpleDateFormat shortDay = new SimpleDateFormat("EE", Locale.GERMAN);
                SimpleDateFormat time = new SimpleDateFormat("HH:mm", Locale.GERMAN);

                weekday = shortDay.format(pem.getBegin()).substring(0, 2);
                start = time.format(pem.getBegin());
                end = time.format(pem.getEnd());

                RouteInformationModel rim = mContext.getRouteInformation();
                if (rim.getDistanceText().equals("")) {
                    kilometer = 0;
                } else {
                    String[] withMeasurement = rim.getDistanceText().split(" ");
                    kilometer = (int) Double.parseDouble(withMeasurement[0]);
                }

                // Build string for one day
                day = "{" +
                        "\"Wochentag\": \"" + weekday + "\"," +
                        "\"Abfahrtszeit\": \"" + start + "\"," +
                        "\"Kilometer\": " + kilometer + "," +
                        "\"Ankunftszeit\": \"" + end + "\"," +
                        "\"Zusatz km\": 0" +
                        "}";
            } else {
                // Get the weekday shortcut of the day that has no data to sync
                if (i == 0) {
                    weekday = "Mo";
                } else if (i == 1) {
                    weekday = "Di";
                } else if (i == 2) {
                    weekday = "Mi";
                } else if (i == 3) {
                    weekday = "Do";
                } else if (i == 4) {
                    weekday = "Fr";
                } else if (i == 5) {
                    weekday = "Sa";
                } else {
                    weekday = "So";
                }

                // Build string for day with no data
                day = "{" +
                        "\"Wochentag\": \"" + weekday + "\"," +
                        "\"Abfahrtszeit\": \"00:00\"," +
                        "\"Kilometer\": 0," +
                        "\"Ankunftszeit\": \"00:00\"," +
                        "\"Zusatz km\": 0" +
                        "}";
            }

            mData.append(day);
            if (i != 6) {
                mData.append(",");
            }
        }

        mData.append("]");

        // Return empty string when an error occured
        if(error) {
            return "";
        }

        return mData.toString();
    }
}
