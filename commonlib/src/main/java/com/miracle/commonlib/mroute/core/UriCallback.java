package com.miracle.commonlib.mroute.core;

public interface UriCallback {
    void onNext();

    void onComplete(int resultCode);
}
