package com.miracle.commonlib.mroute;

import android.os.Looper;

import com.miracle.commonlib.mroute.core.RootUriHandler;
import com.miracle.commonlib.mroute.core.UriRequest;

public class MRoute {
    private static RootUriHandler ROOT_HANDLER;

    public static void init(RootUriHandler rootUriHandler) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new RuntimeException("请在主线程初始化");
        }
        if (ROOT_HANDLER == null) {
            ROOT_HANDLER = rootUriHandler;
        }
    }

    public static RootUriHandler getRootHandler() {
        if (ROOT_HANDLER == null) {
            throw new RuntimeException("请先调用init初始化");
        }
        return ROOT_HANDLER;
    }

    public static void startUri(UriRequest request) {
        getRootHandler().startUri(request);
    }
}
