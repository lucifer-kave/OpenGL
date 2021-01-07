package com.miracle.commonlib.mroute.core;

public abstract class UriHandler {
    protected ChainedInterceptor mInterceptor;

    public UriHandler addInterceptor(UriInterceptor interceptor) {
        if (interceptor != null) {
            if (mInterceptor == null) {
                mInterceptor = new ChainedInterceptor();
            }
            mInterceptor.addInterceptor(interceptor);
        }
        return this;
    }

    protected void handle(final UriRequest request, final UriCallback callback) {
        if (shouldHandle(request)) {
            if (mInterceptor != null) {
                mInterceptor.interceptor(request, new UriCallback() {
                    @Override
                    public void onNext() {
                        handlerInternal(request, callback);
                    }

                    @Override
                    public void onComplete(int resultCode) {
                        callback.onComplete(resultCode);
                    }
                });
            }
        } else {
            callback.onNext();
        }
    }

    public abstract boolean shouldHandle(UriRequest request);

    public abstract void handlerInternal(UriRequest request, UriCallback callback);
}
