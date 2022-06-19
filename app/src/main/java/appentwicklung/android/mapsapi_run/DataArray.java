package appentwicklung.android.mapsapi_run;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import appentwicklung.android.mapsapi_run.helper.HelperUtils;
import appentwicklung.android.mapsapi_run.model.WorkoutSession;

public class DataArray extends ArrayAdapter<WorkoutSession> {

    public DataArray(Context context, List<WorkoutSession> sessions) {
        super(context, 0, sessions);
    }

    /**
     * Constructor
     *
     * @param context  The current context.
     * @param resource The resource ID for a layout file containing a TextView to use when
     */
    public DataArray(@NonNull Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //Gets the data item for this position
        WorkoutSession session = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.mussumbenann, parent, false);
        }

        TextView textviewSessionId = (TextView) convertView.findViewById(R.id.textviewSessionId);
        TextView textviewSessionStartDate = (TextView) convertView.findViewById(R.id.textviewSessionStartDate);
        TextView textviewSessionStartTime = (TextView) convertView.findViewById(R.id.textviewSessionStartTime);
        TextView textviewSessionDuration = (TextView) convertView.findViewById(R.id.textviewSessionDuration);
        TextView textviewSessionDistance = (TextView) convertView.findViewById(R.id.textviewSessionDistance);

        //Sets the text in the textviews
        textviewSessionId.setText("Session " + Integer.toString(session.getId()) + ":");
        textviewSessionStartDate.setText(session.getStartDate());
        textviewSessionStartTime.setText(session.getStartTime());
        textviewSessionDuration.setText(DateUtils.formatElapsedTime(session.getDuration()));

        //Shows the distance in km, formatted with 1 decimal
        float distance = HelperUtils.formatFloat(session.getDistance() / 1000);
        textviewSessionDistance.setText(Float.toString(distance) + " km");

        return convertView;

    }
}
