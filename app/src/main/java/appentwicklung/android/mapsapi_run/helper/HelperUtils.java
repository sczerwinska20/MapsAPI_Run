package appentwicklung.android.mapsapi_run.helper;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import appentwicklung.android.mapsapi_run.model.WorkoutLocation;
import appentwicklung.android.mapsapi_run.model.WorkoutSession;

/**
 * Class for helper methods
 *
 * @author Daniel Johansson
 */
public class HelperUtils {

    /**
     * Calculates the average speed of the WorkoutSession
     *
     * @param session WorkoutSession
     * @return average speed of the WorkoutSession
     */
    public static double calculateAverageSpeed(WorkoutSession session) {
        double avgSpeed = 0;

        List<WorkoutLocation> locations = session.getLocations();
        WorkoutLocation location = locations.get(locations.size() - 1);         //Gets last element

        double distance = session.getDistance() / 1000;              //Distance in km

        //AvgSpeed=Distance in km divided by elapsed time in hours
        avgSpeed = distance / (((double) location.getElapsedTime()) / 60 / 60);

        return formatDouble(avgSpeed);              //Formats avgSpeed with 1 decimal and returns it
    }

    /**
     * Calculates max speed of the WorkoutSession
     *
     * @param session WorkoutSession
     * @return max speed of the WorkoutSession
     */
    public static float getMaxSpeed(WorkoutSession session) {
        float maxSpeed = 0;

        List<WorkoutLocation> locations = session.getLocations();

        //Compares speed of all WorkoutLocations in the WorkoutSession, and sets the max speed
        for (WorkoutLocation location : locations) {
            float speed = location.getSpeed();

            if (speed > maxSpeed) {
                maxSpeed = speed;
            }
        }
        return formatFloat(maxSpeed);           //Formats max speed with 1 decimal and returns it
    }

    /**
     * Converts the speed from metres/second to kilometres/hour
     *
     * @param speed speed to be converted
     * @return converted speed
     */
    public static float convertSpeed(float speed) {
        return (speed * 18) / 5;
    }

    /**
     * Formats the number with 1 decimal (rounded up), and removes any commas in the number
     *
     * @param number number to be formatted
     * @return formatted number
     */
    public static float formatFloat(float number) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMaximumFractionDigits(1);          //Sets max decimals to 1
        formatter.setMinimumFractionDigits(1);
        formatter.setRoundingMode(RoundingMode.HALF_UP);    //Sets rounding mode

        //Format number with 1 decimal, rounded up
        String formattedString = formatter.format(number);

        //Replace any commas in number
        String stringWithoutCommas = formattedString.replaceAll(",", "");

        return Float.parseFloat(stringWithoutCommas);
    }

    /**
     * Formats the number with 1 decimal (rounded up), and removes any commas in the number
     *
     * @param number number to be formatted
     * @return formatted number
     */
    public static double formatDouble(double number) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMaximumFractionDigits(1);                  //Sets max decimals to 1
        formatter.setMinimumFractionDigits(1);
        formatter.setRoundingMode(RoundingMode.HALF_UP);        //Sets rounding mode

        //Format number with 1 decimal, rounded up
        String formattedString = formatter.format(number);

        //Replace any commas in number
        String stringWithoutCommas = formattedString.replaceAll(",", "");

        return Double.parseDouble(stringWithoutCommas);
    }
}
