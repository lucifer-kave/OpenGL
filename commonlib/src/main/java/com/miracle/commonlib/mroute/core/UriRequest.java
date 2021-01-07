package com.miracle.commonlib.mroute.core;

import android.content.Context;

import com.miracle.commonlib.mroute.MRoute;

public class UriRequest {
    private Context mContext;
    private String mPath;

    public UriRequest(Context context, String path) {
        mContext = context;
        mPath = path;
    }

    public void start() {
        MRoute.startUri(this);
    }
}
