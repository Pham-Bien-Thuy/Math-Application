package com.example.ttv.data;

import com.google.gson.annotations.SerializedName;

public class Image {
    @SerializedName("src")
    private String sourceLink;

    public String getSourceLink(){
        return sourceLink;
    }

}
