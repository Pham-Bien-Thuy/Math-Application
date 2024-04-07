package com.example.ttv.chatbox;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class ObjectData implements Parcelable{
    private String objName;
    private Bitmap objImage;
    private String objImageURL; // Added URL field


    public ObjectData(String objName, Bitmap objImage) {
        this.objName = objName;
        this.objImage = objImage;
    }

    protected ObjectData(Parcel in) {
        objName = in.readString();
        objImage = in.readParcelable(Bitmap.class.getClassLoader());
        objImageURL = in.readString(); // Read the URL from the Parcel

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(objName);
        dest.writeParcelable(objImage, flags);
        dest.writeString(objImageURL); // Write the URL to the Parcel

    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ObjectData> CREATOR = new Creator<ObjectData>() {
        @Override
        public ObjectData createFromParcel(Parcel in) {
            return new ObjectData(in);
        }

        @Override
        public ObjectData[] newArray(int size) {
            return new ObjectData[size];
        }
    };

    public String getObjName() {
        return objName;
    }

    public Bitmap getObjImage() {
        return objImage;
    }

    public void setObjName(String newName) {
        this.objName = newName;

    }
    public void setObjImageURL(String objImageURL) {
        this.objImageURL = objImageURL;
    }


    public String getObjImageURL() {
        return objImageURL;
    }
}

