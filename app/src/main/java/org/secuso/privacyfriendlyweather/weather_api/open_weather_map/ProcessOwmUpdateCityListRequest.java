package org.secuso.privacyfriendlyweather.weather_api.open_weather_map;

import android.content.Context;
import android.widget.Toast;

import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.secuso.privacyfriendlyweather.R;
import org.secuso.privacyfriendlyweather.orm.CurrentWeatherData;
import org.secuso.privacyfriendlyweather.orm.DatabaseHelper;
import org.secuso.privacyfriendlyweather.ui.UiUpdater;
import org.secuso.privacyfriendlyweather.weather_api.IDataExtractor;
import org.secuso.privacyfriendlyweather.weather_api.IProcessHttpRequest;

import java.sql.SQLException;

/**
 * This class processes the HTTP requests that are made to the OpenWeatherMap API requesting the
 * current weather for all stored cities.
 */
public class ProcessOwmUpdateCityListRequest implements IProcessHttpRequest {

    /**
     * Constants
     */
    private final String DEBUG_TAG = "process_update_list";

    /**
     * Member variables
     */
    private Context context;
    private DatabaseHelper dbHelper;

    /**
     * Constructor.
     *
     * @param context The context of the HTTP request.
     */
    public ProcessOwmUpdateCityListRequest(Context context, DatabaseHelper dbHelper) {
        this.context = context;
        this.dbHelper = dbHelper;
    }

    /**
     * Converts the response to JSON and updates the database so that the latest weather data are
     * displayed.
     *
     * @param response The response of the HTTP request.
     */
    @Override
    public void processSuccessScenario(String response) {
        IDataExtractor extractor = new OwmDataExtractor();
        try {
            JSONObject json = new JSONObject(response);
            JSONArray list = json.getJSONArray("list");
            for (int i = 0; i < list.length(); i++) {
                String currentItem = list.get(i).toString();
                CurrentWeatherData weatherData = extractor.extractCurrentWeatherData(currentItem);
                int cityId = extractor.extractCityID(currentItem);
                // Data were not well-formed, abort
                if (weatherData == null || cityId == Integer.MIN_VALUE) {
                    final String ERROR_MSG = context.getResources().getString(R.string.convert_to_json_error);
                    Toast.makeText(context, ERROR_MSG, Toast.LENGTH_LONG).show();
                    return;
                }
                // Could retrieve all data, so proceed
                else {
                    weatherData.setCity(dbHelper.getCityByCityID(cityId));
                    // TODO: Handle the case when the city is null: Extract the data from the response and create a new City record
                    try {
                        dbHelper.getCurrentWeatherDataDao().create(weatherData);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        final String ERROR_MSG = context.getResources().getString(R.string.insert_into_db_error);
                        Toast.makeText(context, ERROR_MSG, Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // Update the UI
        UiUpdater uiUpdater = new UiUpdater(context, dbHelper);
        uiUpdater.updateCityList();
    }

    /**
     * Shows an error that the data could not be retrieved.
     *
     * @param error The error that occurred while executing the HTTP request.
     */
    @Override
    public void processFailScenario(VolleyError error) {

    }

}
