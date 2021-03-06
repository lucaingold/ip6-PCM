package ch.fhnw.ip6.powerconsumptionmanager.network;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import ch.fhnw.ip6.powerconsumptionmanager.R;
import ch.fhnw.ip6.powerconsumptionmanager.model.PCMComponent;
import ch.fhnw.ip6.powerconsumptionmanager.model.PCMData;
import ch.fhnw.ip6.powerconsumptionmanager.util.PowerConsumptionManagerAppContext;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Async task to request the cost statistics from the PCM.
 */
public class GetStatisticsAsyncTask extends AsyncTask<Void, Void, Boolean> {
    private static final String TAG = "GetStatsDataAsyncTask";

    private PowerConsumptionManagerAppContext mAppContext;
    private AsyncTaskCallback mCallbackContext;
    private String mURL;
    private PCMData mPCMData;

    /**
     * Constructor to create a new task to request the cost statistics of the PCM.
     * @param appContext Application context.
     * @param callbackContext Context of the callback.
     */
    public GetStatisticsAsyncTask(PowerConsumptionManagerAppContext appContext, AsyncTaskCallback callbackContext) {
        mAppContext = appContext;
        mCallbackContext = callbackContext;
        mURL = "http://" + mAppContext.getIPAdress() + ":";
        mPCMData = mAppContext.getPCMData();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        boolean successGeneralStatistics;
        boolean successComponentStatistics;

        /* The cost statistics need to be requested over two separate webservices. One delivers all cost statistics to self supply,
         * surplus, ... and the other one the cost statistics per component.
         */
        Request generalStatisticsData = new Request.Builder()
                .url(mURL + mAppContext.getString(R.string.webservice_getCostStatisticsGeneral) + "?NumDays=" + mAppContext.getCostStatisticsPeriod())
                .build();
        Request componentStatisticsData = new Request.Builder()
                .url(mURL + mAppContext.getString(R.string.webservice_getCostStatisticsComponents) + "?NumDays=" + mAppContext.getCostStatisticsPeriod())
                .build();

        Response response;
        // Clear maybe previously requested cost statistics data
        mPCMData.clearStatistics();

        try {
            // Request the general cost statistics like self supply, surplus ... and process the response
            response = mAppContext.getOkHTTPClient().newCall(generalStatisticsData).execute();
            if(!response.isSuccessful()) {
                Log.e(TAG, "Response for general statistics data not successful.");
                return false;
            }
            successGeneralStatistics = handleGeneralStatisticsResponse(response);
        } catch (IOException e) {
            Log.e(TAG, "Exception while loading general statistics data.");
            successGeneralStatistics = false;
        }

        try {
            // Request the cost statistics for every component and process the response
            response = mAppContext.getOkHTTPClient().newCall(componentStatisticsData).execute();
            if(!response.isSuccessful()) {
                Log.e(TAG, "Response for components statistics data not successful.");
                return false;
            }
            successComponentStatistics = handleComponentStatisticsResponse(response);
        } catch (IOException e) {
            Log.e(TAG, "Exception while loading components statistics data.");
            successComponentStatistics = false;
        }

        return successGeneralStatistics && successComponentStatistics;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        mCallbackContext.asyncTaskFinished(result, mAppContext.OP_TYPES[0]);
    }

    /**
     * Processes the response of the general cost statistics request.
     * @param response The response of the current statistics request.
     * @return State if the response could be processed successfully.
     * @throws IOException
     */
    public boolean handleGeneralStatisticsResponse(Response response) throws IOException {
        boolean success = true;

        try {
            JSONArray dataJson = new JSONArray(response.body().string());
            for(int i = 0; i < dataJson.length(); i++) {
                JSONObject dataJsonEntry = (JSONObject) dataJson.get(i);
                // Fill the requested cost statistics dates list as well on the first loop
                if(i == 0) {
                    mPCMData.fillStatistics(dataJsonEntry.getString("Name"), dataJsonEntry.getJSONArray("Data"), false);
                } else {
                    mPCMData.fillStatistics(dataJsonEntry.getString("Name"), dataJsonEntry.getJSONArray("Data"), true);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON exception while processing general statistics data.");
            success = false;
        }

        return success;
    }

    /**
     * Processes the response of all cost statistics to the connected components.
     * @param response The response of the cost statistics for every component request.
     * @return State if the response could be processed successfully.
     * @throws IOException
     */
    public boolean handleComponentStatisticsResponse(Response response) throws IOException {
        boolean success = true;

        try {
            JSONArray dataJson = new JSONArray(response.body().string());

            for(int i = 0; i < dataJson.length(); i++) {
                JSONObject dataJsonEntry = (JSONObject) dataJson.get(i);
                // Get the component and fill it with the requested cost statistics data
                PCMComponent component = mPCMData.getComponentData().get(dataJsonEntry.getString("Name"));
                if(component != null) {
                    component.fillStatistics(dataJsonEntry.getJSONArray("Data"));
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON exception while processing component statistics data.");
            success = false;
        }

        return success;
    }
}