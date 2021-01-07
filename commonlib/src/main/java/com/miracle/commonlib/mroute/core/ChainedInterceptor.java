package com.miracle.commonlib.mroute.core;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ChainedInterceptor implements UriInterceptor {
    private List<UriInterceptor> mInterceptors = new LinkedList<>();

    public void addInterceptor(UriInterceptor interceptor) {
        if (interceptor != null) {
            mInterceptors.add(interceptor);
        }
    }

    @Override
    public void interceptor(UriRequest request, UriCallback callback) {
        next(mInterceptors.iterator(), request, callback);
    }

    private void next(final Iterator<UriInterceptor> iterator, final UriRequest request, final UriCallback callback) {
        if (iterator.hasNext()) {
            UriInterceptor t = iterator.next();
            t.interceptor(request, new UriCallback() {
                @Override
                public void onNext() {
                    next(iterator, request, callback);
                }

                @Override
                public void onComplete(int resultCode) {
                    callback.onComplete(resultCode);
                }
            });
        } else {
            callback.onNext();
        }
    }
}
