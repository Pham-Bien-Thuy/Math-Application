package com.example.ttv.data;

import com.example.ttv.QueryResult;
import com.google.gson.annotations.SerializedName;

public class WolframResult {
    @SerializedName("queryresult")
    private QueryResult result;

    public QueryResult getResult(){
        return result;
    }
}
