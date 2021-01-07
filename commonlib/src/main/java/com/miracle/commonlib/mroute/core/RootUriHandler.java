package com.miracle.commonlib.mroute.core;

import com.miracle.commonlib.mroute.utils.PriorityList;

import java.util.Iterator;

public class RootUriHandler extends UriHandler {
    private final PriorityList<UriHandler> mHandlers = new PriorityList<>();

    public void startUri(UriRequest request) {
        handle(request, new RootUriCallback(request));
    }

    protected class RootUriCallback implements UriCallback {
        public RootUriCallback(UriRequest request) {
        }

        @Override
        public void onNext() {
            onComplete(UriResult.CODE_NOT_FOUND);
        }

        @Override
        public void onComplete(int resultCode) {

        }
    }

    @Override
    public boolean shouldHandle(UriRequest request) {
        return !mHandlers.isEmpty();
    }

    @Override
    public void handlerInternal(UriRequest request, UriCallback callback) {
        next(mHandlers.iterator(), request, callback);
    }

    private void next(final Iterator<UriHandler> iterator, final UriRequest request, final UriCallback callback) {
        if (iterator.hasNext()) {
            UriHandler t = mHandlers.iterator().next();
            t.handle(request, new UriCallback() {

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
