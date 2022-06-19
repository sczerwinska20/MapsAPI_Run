package appentwicklung.android.mapsapi_run.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Model class for the WorkoutLocation
 *
 * @author Daniel Johansson
 */
public class WorkoutLocation implements Parcelable {

    private int mId;

    private int mSessionId;

    private String mLatitude;

    private String mLongitude;

    private float mSpeed;

    private long mElapsedTime;

    public WorkoutLocation() {
    }

    /**
     * Constructor
     *
     * @param latitude    latitude of the WorkoutLocation
     * @param longitude   longitude of the WorkoutLocation
     * @param speed       speed registered in the WorkoutLocation

     * @param elapsedTime elapsed time of the WorkoutSession, when the WorkoutLocation was registered
     */
    public WorkoutLocation(String latitude, String longitude, float speed,  long elapsedTime) {
        this.mLatitude = latitude;
        this.mLongitude = longitude;
        this.mSpeed = speed;
        this.mElapsedTime = elapsedTime;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public String getLatitude() {
        return mLatitude;
    }

    public void setLatitude(String latitude) {
        this.mLatitude = latitude;
    }

    public String getLongitude() {
        return mLongitude;
    }

    public void setLongitude(String longitude) {
        this.mLongitude = longitude;
    }

    public float getSpeed() {
        return mSpeed;
    }

    public void setSpeed(float speed) {
        this.mSpeed = speed;
    }

    public int getSessionId() {
        return mSessionId;
    }

    public void setSessionId(int sessionId) {
        this.mSessionId = sessionId;
    }

    public long getElapsedTime() {
        return mElapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.mElapsedTime = elapsedTime;
    }

    /**
     * Writes to parcel
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mLatitude);
        out.writeString(mLongitude);
        out.writeFloat(mSpeed);
        out.writeLong(mElapsedTime);
    }

    public static final Creator<WorkoutLocation> CREATOR = new Creator<WorkoutLocation>() {
        public WorkoutLocation createFromParcel(Parcel in) {
            return new WorkoutLocation(in);
        }

        public WorkoutLocation[] newArray(int size) {
            return new WorkoutLocation[size];
        }
    };

    /**
     * Constructor used by Parcelable.Creator
     */
    private WorkoutLocation(Parcel in) {
        mLatitude = in.readString();
        mLongitude = in.readString();
        mSpeed = in.readFloat();
        mElapsedTime = in.readLong();
    }

    public int describeContents() {
        return 0;
    }

}
