#### 1. android 调用js

```java
mBridgeWebview.loadUrl("javascript:nativeFunction('" + data + "')");
```

直接loadUrl中进行拼接调用



#### 2. Js调用android

- 1.在android中注册handler

  ```java
  mBridgeWebview.registerHandler("functionOpen", new BridgeHandler() {
      @Override
      public void handler(String data, CallBackFunction function) {
          Toast.makeText(MainActivity.this, "网页在打开你的文件预览", Toast.LENGTH_SHORT).show();
      }
  });
  ```

  用一个Map存储你注册的handler

  ```java
  Map<String, BridgeHandler> messageHandlers = new HashMap<String, BridgeHandler>();
  ```

  name-handler

- 2. 调用JS方法

  ```javascript
  function useClick() {
          var name = document.getElementById("uname").value;
          var pwd = document.getElementById("psw").value;
          var data = "name = " + name + ", password = " + pwd;
  
          window.WebViewJavascriptBridge.callHandler(
              'submitFromWeb', {'info':data},
              function(responseData) {
                  document.getElementById("show").innerHTML = responseData;
              }
          );
   }
  ```

- 3.WebViewClient拦截

  ```java
  mBridgeWebview.setWebViewClient(new MyWebViewClient(mBridgeWebview, MainActivity.this));
  ```

  ```java
  @Override
  public boolean shouldOverrideUrlLoading(WebView view, String url) {
      Log.i("liuw", "url地址为：" + url);
      try {
          url = URLDecoder.decode(url, "UTF-8");
      } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
      }
      //默认操作url地址yy://__QUEUE_MESSAGE__/
      if (url.trim().startsWith("yy:")) {
        	// 调用父类
          return super.shouldOverrideUrlLoading(view, url);
      }
      //特殊情况tel，调用系统的拨号软件拨号【<a href="tel:110">拨打电话110</a>】
      if(url.trim().startsWith("tel")){
          Intent i = new Intent(Intent.ACTION_VIEW);
          i.setData(Uri.parse(url));
          mContext.startActivity(i);
      }else {
          //特殊情况【调用系统浏览器打开】<a href="https://www.csdn.net">调用系统浏览器</a>
          if(url.contains("csdn")){
              Intent i = new Intent(Intent.ACTION_VIEW);
              i.setData(Uri.parse(url));
              mContext.startActivity(i);
          } else {//其它非特殊情况全部放行
              view.loadUrl(url);
          }
      }
      return true;
  }
  ```

  ```java
  @Override
  public boolean shouldOverrideUrlLoading(WebView view, String url) {
      try {
          url = URLDecoder.decode(url, "UTF-8");
      } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
      }
  
      if (url.startsWith(BridgeUtil.YY_RETURN_DATA)) { // 如果是返回数据
          webView.handlerReturnData(url);
          return true;
      } else if (url.startsWith(BridgeUtil.YY_OVERRIDE_SCHEMA)) { 
        	// 调用flushMessageQueue
          webView.flushMessageQueue();
          return true;
      } else {
          return super.shouldOverrideUrlLoading(view, url);
      }
  }
  ```

​      核心逻辑`webView.flushMessageQueue();`

```java
void flushMessageQueue() {
   if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
     	// 调用`javascript:WebViewJavascriptBridge._fetchQueue();`完成处队操作
     	// 这里load的就是javascript:WebViewJavascriptBridge._fetchQueue()
      loadUrl(BridgeUtil.JS_FETCH_QUEUE_FROM_JAVA, new CallBackFunction() {

         @Override
         public void onCallBack(String data) {
            // deserializeMessage
            List<Message> list = null;
            try {
               // 拿到封装的data里面有注册handler的方法名
               list = Message.toArrayList(data);
            } catch (Exception e) {
                       e.printStackTrace();
               return;
            }
            if (list == null || list.size() == 0) {
               return;
            }
            for (int i = 0; i < list.size(); i++) {
               Message m = list.get(i);
               String responseId = m.getResponseId();
               // 是否是response
               if (!TextUtils.isEmpty(responseId)) {
                  CallBackFunction function = responseCallbacks.get(responseId);
                  String responseData = m.getResponseData();
                  function.onCallBack(responseData);
                  responseCallbacks.remove(responseId);
               } else {
                  CallBackFunction responseFunction = null;
                  // if had callbackId
                  final String callbackId = m.getCallbackId();
                  if (!TextUtils.isEmpty(callbackId)) {
                     responseFunction = new CallBackFunction() {
                        @Override
                        public void onCallBack(String data) {
                           Message responseMsg = new Message();
                           responseMsg.setResponseId(callbackId);
                           responseMsg.setResponseData(data);
                           queueMessage(responseMsg);
                        }
                     };
                  } else {
                     responseFunction = new CallBackFunction() {
                        @Override
                        public void onCallBack(String data) {
                           // do nothing
                        }
                     };
                  }
                  BridgeHandler handler;
                  if (!TextUtils.isEmpty(m.getHandlerName())) {
                     handler = messageHandlers.get(m.getHandlerName());
                  } else {
                     handler = defaultHandler;
                  }
                  if (handler != null){
                     // 完成回调
                     handler.handler(m.getData(), responseFunction);
                  }
               }
            }
         }
      });
   }
}
```

调用`javascript:WebViewJavascriptBridge._fetchQueue();`完成处队操作