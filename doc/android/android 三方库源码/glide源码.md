#### 1. glide概述

(1)支持Memory和Disk图片缓存。
(2)支持gif和webp格式图片。
(3)根据Activity/Fragment生命周期自动管理请求。
(4)使用Bitmap Pool可以使Bitmap复用。
(5)对于回收的Bitmap会主动调用recycle，减小系统回收压力。

```jav&#39;a
GlideApp.with(this)
        //.asBitmap()
        //.asDrawable()
        //.asGif()
        .load("xxxx")
        .placeholder(R.mipmap.ic_launcher_round)
        .error(R.mipmap.error)
        .fitCenter()
        .into(mIv);
```

glide 4-x通过注解会在apt生成部分外部文件来操作glide,大大提高了glide的可定义性

#### 2. glide的初始化`GlideApp.with`

`GlideApp.with`

```java
@NonNull
public static GlideRequests with(@NonNull FragmentActivity activity) {
  return (GlideRequests) Glide.with(activity);
}
```

```java
@NonNull
public static RequestManager with(@NonNull FragmentActivity activity) {
  return getRetriever(activity).get(activity);
}
```

`getRetriever(activity).get(activity);`是glide的初始化同时也是对生命周期的绑定

`getRetriever(activity)`

```java
@NonNull
private static RequestManagerRetriever getRetriever(@Nullable Context context) {
  // Context could be null for other reasons (ie the user passes in null), but in practice it will
  // only occur due to errors with the Fragment lifecycle.
  Preconditions.checkNotNull(
      context,
      "You cannot start a load on a not yet attached View or a Fragment where getActivity() "
          + "returns null (which usually occurs when getActivity() is called before the Fragment "
          + "is attached or after the Fragment is destroyed).");
  return Glide.get(context).getRequestManagerRetriever();
}
```

这是glide的初始化,我们来看`Glide.get(context)`

```java
@NonNull
public static Glide get(@NonNull Context context) {
  if (glide == null) {
    synchronized (Glide.class) {
      if (glide == null) {
        checkAndInitializeGlide(context);
      }
    }
  }

  return glide;
}
```

这是一个单例模式,继续往下看:

```java
private static void checkAndInitializeGlide(@NonNull Context context) {
  // In the thread running initGlide(), one or more classes may call Glide.get(context).
  // Without this check, those calls could trigger infinite recursion.
  if (isInitializing) {
    throw new IllegalStateException("You cannot call Glide.get() in registerComponents(),"
        + " use the provided Glide instance instead");
  }
  isInitializing = true;
  initializeGlide(context);
  isInitializing = false;
}
```

继续看`initializeGlide(context)`

```java
private static void initializeGlide(@NonNull Context context) {
  initializeGlide(context, new GlideBuilder());
}

@SuppressWarnings("deprecation")
private static void initializeGlide(@NonNull Context context, @NonNull GlideBuilder builder) {
  Context applicationContext = context.getApplicationContext();
  GeneratedAppGlideModule annotationGeneratedModule = getAnnotationGeneratedGlideModules();
  List<com.bumptech.glide.module.GlideModule> manifestModules = Collections.emptyList();
  if (annotationGeneratedModule == null || annotationGeneratedModule.isManifestParsingEnabled()) {
    manifestModules = new ManifestParser(applicationContext).parse();
  }

  if (annotationGeneratedModule != null
      && !annotationGeneratedModule.getExcludedModuleClasses().isEmpty()) {
    Set<Class<?>> excludedModuleClasses =
        annotationGeneratedModule.getExcludedModuleClasses();
    Iterator<com.bumptech.glide.module.GlideModule> iterator = manifestModules.iterator();
    while (iterator.hasNext()) {
      com.bumptech.glide.module.GlideModule current = iterator.next();
      if (!excludedModuleClasses.contains(current.getClass())) {
        continue;
      }
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "AppGlideModule excludes manifest GlideModule: " + current);
      }
      iterator.remove();
    }
  }

  if (Log.isLoggable(TAG, Log.DEBUG)) {
    for (com.bumptech.glide.module.GlideModule glideModule : manifestModules) {
      Log.d(TAG, "Discovered GlideModule from manifest: " + glideModule.getClass());
    }
  }

  RequestManagerRetriever.RequestManagerFactory factory =
      annotationGeneratedModule != null
          ? annotationGeneratedModule.getRequestManagerFactory() : null;
  builder.setRequestManagerFactory(factory);
  for (com.bumptech.glide.module.GlideModule module : manifestModules) {
    module.applyOptions(applicationContext, builder);
  }
  if (annotationGeneratedModule != null) {
    annotationGeneratedModule.applyOptions(applicationContext, builder);
  }
  // 核心代码
  Glide glide = builder.build(applicationContext);
  for (com.bumptech.glide.module.GlideModule module : manifestModules) {
    module.registerComponents(applicationContext, glide, glide.registry);
  }
  if (annotationGeneratedModule != null) {
    annotationGeneratedModule.registerComponents(applicationContext, glide, glide.registry);
  }
  applicationContext.registerComponentCallbacks(glide);
  Glide.glide = glide;
}
```

大部分是一些注解相关的逻辑,接着我们来看glide的初始化代码:`Glide glide = builder.build(applicationContext);`

```java
@NonNull
Glide build(@NonNull Context context) {
  if (sourceExecutor == null) {
    // 资源线程池
    sourceExecutor = GlideExecutor.newSourceExecutor();
  }

  if (diskCacheExecutor == null) {
    // 磁盘缓存线程池
    diskCacheExecutor = GlideExecutor.newDiskCacheExecutor();
  }

  if (animationExecutor == null) {
    // 动画线程池
    animationExecutor = GlideExecutor.newAnimationExecutor();
  }

  if (memorySizeCalculator == null) {
    memorySizeCalculator = new MemorySizeCalculator.Builder(context).build();
  }

  if (connectivityMonitorFactory == null) {
    connectivityMonitorFactory = new DefaultConnectivityMonitorFactory();
  }

  if (bitmapPool == null) {
    int size = memorySizeCalculator.getBitmapPoolSize();
    if (size > 0) {
      bitmapPool = new LruBitmapPool(size);
    } else {
      bitmapPool = new BitmapPoolAdapter();
    }
  }

  if (arrayPool == null) {
    arrayPool = new LruArrayPool(memorySizeCalculator.getArrayPoolSizeInBytes());
  }

  if (memoryCache == null) {
    memoryCache = new LruResourceCache(memorySizeCalculator.getMemoryCacheSize());
  }

  if (diskCacheFactory == null) {
    diskCacheFactory = new InternalCacheDiskCacheFactory(context);
  }

  if (engine == null) {
    engine =
        new Engine(
            memoryCache,
            diskCacheFactory,
            diskCacheExecutor,
            sourceExecutor,
            GlideExecutor.newUnlimitedSourceExecutor(),
            GlideExecutor.newAnimationExecutor(),
            isActiveResourceRetentionAllowed);
  }

  RequestManagerRetriever requestManagerRetriever =
      new RequestManagerRetriever(requestManagerFactory);

  return new Glide(
      context,
      engine,
      memoryCache,
      bitmapPool,
      arrayPool,
      requestManagerRetriever,
      connectivityMonitorFactory,
      logLevel,
      defaultRequestOptions.lock(),
      defaultTransitionOptions);
}
```

到这里glide初始化完成:

初始化了  BitmapPool,图片加载引擎engin等

#### 3. glide周期绑定

`getRetriever(activity).get(activity)`我们来看`.get(activity)`的源码:

```java
@NonNull
public RequestManager get(@NonNull FragmentActivity activity) {
  if (Util.isOnBackgroundThread()) {
    // 非主线程执行
    return get(activity.getApplicationContext());
  } else {
    // 主线程执行
    assertNotDestroyed(activity);
    FragmentManager fm = activity.getSupportFragmentManager();
    return supportFragmentGet(
        activity, fm, /*parentHint=*/ null, isActivityVisible(activity));
  }
}
```

这里传入一个上下文对象(或者说Activity)看起来莫名其妙,但是有一个线程的区分  **主线程 & 非主线程**

我们来看**后台线程**调用的逻辑:

```java
@NonNull
public RequestManager get(@NonNull Context context) {
  if (context == null) {
    throw new IllegalArgumentException("You cannot start a load on a null Context");
  } else if (Util.isOnMainThread() && !(context instanceof Application)) {
    if (context instanceof FragmentActivity) {
      return get((FragmentActivity) context);
    } else if (context instanceof Activity) {
      return get((Activity) context);
    } else if (context instanceof ContextWrapper) {
      return get(((ContextWrapper) context).getBaseContext());
    }
  }

  return getApplicationManager(context);
}
```

因为是非主线程,所以get的入参是`activity.getApplicationContext()`所以上诉代码仅仅会走到`getApplicationManager(context);`

```java
@NonNull
private RequestManager getApplicationManager(@NonNull Context context) {
  // Either an application context or we're on a background thread.
  if (applicationManager == null) {
    synchronized (this) {
      if (applicationManager == null) {
        // Normally pause/resume is taken care of by the fragment we add to the fragment or
        // activity. However, in this case since the manager attached to the application will not
        // receive lifecycle events, we must force the manager to start resumed using
        // ApplicationLifecycle.

        // TODO(b/27524013): Factor out this Glide.get() call.
        Glide glide = Glide.get(context.getApplicationContext());
        applicationManager =
            factory.build(
                glide,
                new ApplicationLifecycle(),
                new EmptyRequestManagerTreeNode(),
                context.getApplicationContext());
      }
    }
  }

  return applicationManager;
}
```



通常，暂停/恢复由我们添加到片段或活动中的片段处理。但是，在这种情况下(绑定Application或者在后台执行)，由于附加到应用程序的管理器将不接收生命周期事件，**因此必须强制管理器使用ApplicationLifecycle开始恢复**

##### 3.1 主线程下传入 Activity

```java
@NonNull
public RequestManager get(@NonNull Activity activity) {
  if (Util.isOnBackgroundThread()) {
    return get(activity.getApplicationContext());
  } else {
    // 判断Activity是否销毁
    assertNotDestroyed(activity);
    android.app.FragmentManager fm = activity.getFragmentManager();
    return fragmentGet(
        activity, fm, /*parentHint=*/ null, isActivityVisible(activity));
  }
}
```

创建一个fragment作为碎片进行周期管理

##### 3.2 主线程下传入 FragmentActivity

与传入Activity一致

#####3.3 主线程传入fragment

```java
@NonNull
public RequestManager get(@NonNull Fragment fragment) {
  Preconditions.checkNotNull(fragment.getActivity(),
        "You cannot start a load on a fragment before it is attached or after it is destroyed");
  if (Util.isOnBackgroundThread()) {
    return get(fragment.getActivity().getApplicationContext());
  } else {
    FragmentManager fm = fragment.getChildFragmentManager();
    return supportFragmentGet(fragment.getActivity(), fm, fragment, fragment.isVisible());
  }
}
```

会使用fragment的`getChildFragmentManager`创建一个fragment的子fragment作为碎片管理glide的生命周期

##### 3.4 主线程传入View

```java
@SuppressWarnings("deprecation")
@NonNull
public RequestManager get(@NonNull View view) {
  if (Util.isOnBackgroundThread()) {
    return get(view.getContext().getApplicationContext());
  }

  Preconditions.checkNotNull(view);
  Preconditions.checkNotNull(view.getContext(),
      "Unable to obtain a request manager for a view without a Context");
  Activity activity = findActivity(view.getContext());
  // The view might be somewhere else, like a service.
  if (activity == null) {
    return get(view.getContext().getApplicationContext());
  }

  // Support Fragments.
  // Although the user might have non-support Fragments attached to FragmentActivity, searching
  // for non-support Fragments is so expensive pre O and that should be rare enough that we
  // prefer to just fall back to the Activity directly.
  if (activity instanceof FragmentActivity) {
    Fragment fragment = findSupportFragment(view, (FragmentActivity) activity);
    return fragment != null ? get(fragment) : get(activity);
  }

  // Standard Fragments.
  android.app.Fragment fragment = findFragment(view, activity);
  if (fragment == null) {
    return get(activity);
  }
  return get(fragment);
}
```

同样如果不是主线程还是强制使用**ApplicationLifecycle**,如果是主线程,则会通过View绑定的Activity或者Fragment作为碎片进行周期管理

##### 3.5 主线程传入context

```java
@NonNull
public RequestManager get(@NonNull Context context) {
  if (context == null) {
    throw new IllegalArgumentException("You cannot start a load on a null Context");
  } else if (Util.isOnMainThread() && !(context instanceof Application)) {
    if (context instanceof FragmentActivity) {
      return get((FragmentActivity) context);
    } else if (context instanceof Activity) {
      return get((Activity) context);
    } else if (context instanceof ContextWrapper) {
      return get(((ContextWrapper) context).getBaseContext());
    }
  }

  return getApplicationManager(context);
}
```

先判断context的实现类是`FragmentActivity,Activity还是context`然后调用前面的方法

那么究竟如何进行后序的周期控制,我们继续看;

##### 3.6 周期控制

以Activity为例:

```java
@NonNull
public static RequestManager with(@NonNull Activity activity) {
  return getRetriever(activity).get(activity);
}
```

```java
@NonNull
public RequestManager get(@NonNull Activity activity) {
  if (Util.isOnBackgroundThread()) {
    return get(activity.getApplicationContext());
  } else {
    assertNotDestroyed(activity);
    android.app.FragmentManager fm = activity.getFragmentManager();
    return fragmentGet(
        activity, fm, /*parentHint=*/ null, isActivityVisible(activity));
  }
}
```

```java
@Deprecated
@NonNull
private RequestManager fragmentGet(@NonNull Context context,
    @NonNull android.app.FragmentManager fm,
    @Nullable android.app.Fragment parentHint,
    boolean isParentVisible) {
  RequestManagerFragment current = getRequestManagerFragment(fm, parentHint, isParentVisible);
  RequestManager requestManager = current.getRequestManager();
  if (requestManager == null) {
    // TODO(b/27524013): Factor out this Glide.get() call.
    Glide glide = Glide.get(context);
    requestManager =
        factory.build(
            glide, current.getGlideLifecycle(), current.getRequestManagerTreeNode(), context);
    current.setRequestManager(requestManager);
  }
  return requestManager;
}
```

我们来看核心代码

`  RequestManagerFragment current = getRequestManagerFragment(fm, parentHint,isParentVisible);`

然后继续看`getRequestManagerFragment`

```java
@NonNull
private RequestManagerFragment getRequestManagerFragment(
    @NonNull final android.app.FragmentManager fm,
    @Nullable android.app.Fragment parentHint,
    boolean isParentVisible) {
  RequestManagerFragment current = (RequestManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);
  if (current == null) {
    current = pendingRequestManagerFragments.get(fm);
    if (current == null) {
      current = new RequestManagerFragment();
      current.setParentFragmentHint(parentHint);
      if (isParentVisible) {
        current.getGlideLifecycle().onStart();
      }
      pendingRequestManagerFragments.put(fm, current);
      fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();
      handler.obtainMessage(ID_REMOVE_FRAGMENT_MANAGER, fm).sendToTarget();
    }
  }
  return current;
}
```

这里就是获取创建的**周期碎片**`RequestManagerFragment`与Activity进行绑定,同理如果`glide.with`的入参是fragment同样使用事务与其绑定

然后绑定完成,也就是周期同步之后,发一个handler,把SupportRequestManagerFragment从缓存队列pendingSupportRequestManagerFragments中移除,进行加载;

```java
@Override
public boolean handleMessage(Message message) {
  boolean handled = true;
  Object removed = null;
  Object key = null;
  switch (message.what) {
    case ID_REMOVE_FRAGMENT_MANAGER:
      android.app.FragmentManager fm = (android.app.FragmentManager) message.obj;
      key = fm;
      removed = pendingRequestManagerFragments.remove(fm);
      break;
    case ID_REMOVE_SUPPORT_FRAGMENT_MANAGER:
      FragmentManager supportFm = (FragmentManager) message.obj;
      key = supportFm;
      removed = pendingSupportRequestManagerFragments.remove(supportFm);
      break;
    default:
      handled = false;
      break;
  }
  if (handled && removed == null && Log.isLoggable(TAG, Log.WARN)) {
    Log.w(TAG, "Failed to remove expected request manager fragment, manager: " + key);
  }
  return handled;
}
```

然后继续看`fragmentGet`

```java
@Deprecated
@NonNull
private RequestManager fragmentGet(@NonNull Context context,
    @NonNull android.app.FragmentManager fm,
    @Nullable android.app.Fragment parentHint,
    boolean isParentVisible) {
  RequestManagerFragment current = getRequestManagerFragment(fm, parentHint, isParentVisible);
  RequestManager requestManager = current.getRequestManager();
  if (requestManager == null) {
    // TODO(b/27524013): Factor out this Glide.get() call.
    Glide glide = Glide.get(context);
    requestManager =
        factory.build(
            glide, current.getGlideLifecycle(), current.getRequestManagerTreeNode(), context);
    current.setRequestManager(requestManager);
  }
  return requestManager;
}
```

我们将获取到的**已经绑定完成fragment**获取RequestManager,如果`requestManager == null`,那么我们通过下面代码为当前的fragment设置RequestManager

进行创建最后set到SupportRequestManagerFragment中;注意其参数.current.getGlideLifecycle()其实就是这个接口来回调SupportRequestManagerFragment的周期方法进而进行周期的控制;



#### 4. glide的相关配置

*load()*

```java
  @Override
  @NonNull
  @CheckResult
  public GlideRequest<Drawable> load(@Nullable Bitmap bitmap) {
    return (GlideRequest<Drawable>) super.load(bitmap);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<Drawable> load(@Nullable Drawable drawable) {
    return (GlideRequest<Drawable>) super.load(drawable);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<Drawable> load(@Nullable String string) {
    return (GlideRequest<Drawable>) super.load(string);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<Drawable> load(@Nullable Uri uri) {
    return (GlideRequest<Drawable>) super.load(uri);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<Drawable> load(@Nullable File file) {
    return (GlideRequest<Drawable>) super.load(file);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<Drawable> load(@RawRes @DrawableRes @Nullable Integer id) {
    return (GlideRequest<Drawable>) super.load(id);
  }

  @Override
  @Deprecated
  @CheckResult
  public GlideRequest<Drawable> load(@Nullable URL url) {
    return (GlideRequest<Drawable>) super.load(url);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<Drawable> load(@Nullable byte[] bytes) {
    return (GlideRequest<Drawable>) super.load(bytes);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<Drawable> load(@Nullable Object o) {
    return (GlideRequest<Drawable>) super.load(o);
  }
```

由以上代码中可以看到load有很多实现类,最后都是在RequestBuilder中初始化参数model,在into的时候进行加载;
接着看Glide对图片属性的支持:

```java
GlideApp.with(this)
        //.asBitmap()
        //.asDrawable()
        //.asGif()
        .load("xxxx")
        .placeholder(R.mipmap.ic_launcher_round)
        .error(R.mipmap.error)
        .fitCenter()
        .into(mIv);
```

一般Glide通过一系列链式来配置图片的属性,和加载的属性;当然也可以通过RequestOption来自定义(把相关配置apply进去),最后把RequestOption apply到RequstBuilder中;

我们看下`.asGif`

```java
@Override
@NonNull
@CheckResult
public GlideRequest<GifDrawable> asGif() {
  return (GlideRequest<GifDrawable>) super.asGif();
}
```

```java
private static final RequestOptions DECODE_TYPE_GIF = decodeTypeOf(GifDrawable.class).lock();
@NonNull
@CheckResult
public RequestBuilder<GifDrawable> asGif() {
  return as(GifDrawable.class).apply(DECODE_TYPE_GIF);
}
```

从以上代码可以看到decodeTypeOf(GifDrawable.class).lock();转化为一个GifDrawable之后apply到RequstBuilder中,看到这里是不是感觉RequstBuilder就是一个总指挥,主导Glide的大部分行为(如下 附decodeTypeOf的源码)

```java
 @NonNull
  @CheckResult
  public static RequestOptions decodeTypeOf(@NonNull Class<?> resourceClass) {
    return new RequestOptions().decode(resourceClass);
  }

```

当这里我们对RequestOptions这个原型有了一定的了解;
当然你可以参阅文章开头的Glide v4官网来看其使用,慢慢体会这个思想;

#### 5.glide加载与最终实现

也就是glide加载的最后一部`into()`

```java
@NonNull
public ViewTarget<ImageView, TranscodeType> into(@NonNull ImageView view) {
  Util.assertMainThread();
  Preconditions.checkNotNull(view);

  RequestOptions requestOptions = this.requestOptions;
  if (!requestOptions.isTransformationSet()
      && requestOptions.isTransformationAllowed()
      && view.getScaleType() != null) {
    // Clone in this method so that if we use this RequestBuilder to load into a View and then
    // into a different target, we don't retain the transformation applied based on the previous
    // View's scale type.
    switch (view.getScaleType()) {
      // 图片样式的设置 依赖与requestOptions
      case CENTER_CROP:
        requestOptions = requestOptions.clone().optionalCenterCrop();
        break;
      case CENTER_INSIDE:
        requestOptions = requestOptions.clone().optionalCenterInside();
        break;
      case FIT_CENTER:
      case FIT_START:
      case FIT_END:
        requestOptions = requestOptions.clone().optionalFitCenter();
        break;
      case FIT_XY:
        requestOptions = requestOptions.clone().optionalCenterInside();
        break;
      case CENTER:
      case MATRIX:
      default:
        // Do nothing.
    }
  }

  return into(
      glideContext.buildImageViewTarget(view, transcodeClass),
      /*targetListener=*/ null,
      requestOptions);
}
```

首先是通过之前保存的`requestOptions`来设置图片样式

接着我们看

```java
return into(
        glideContext.buildImageViewTarget(view, transcodeClass),
        /*targetListener=*/ null,
        requestOptions);
```

我们接着看`into()的源码`

```java
private <Y extends Target<TranscodeType>> Y into(
    @NonNull Y target,
    @Nullable RequestListener<TranscodeType> targetListener,
    @NonNull RequestOptions options) {
  Util.assertMainThread();
  Preconditions.checkNotNull(target);
  if (!isModelSet) {
    throw new IllegalArgumentException("You must call #load() before calling #into()");
  }

  options = options.autoClone();
  Request request = buildRequest(target, targetListener, options);
	// 持有先前的request
  Request previous = target.getRequest();
  if (request.isEquivalentTo(previous)
      && !isSkipMemoryCacheWithCompletePreviousRequest(options, previous)) {
    // 回收request
    request.recycle();
    // If the request is completed, beginning again will ensure the result is re-delivered,
    // triggering RequestListeners and Targets. If the request is failed, beginning again will
    // restart the request, giving it another chance to complete. If the request is already
    // running, we can let it continue running without interruption.
    if (!Preconditions.checkNotNull(previous).isRunning()) {
      // Use the previous request rather than the new one to allow for optimizations like skipping
      // setting placeholders, tracking and un-tracking Targets, and obtaining View dimensions
      // that are done in the individual Request.
      // 开始加载之前的request
      previous.begin();
    }
    return target;
  }

  requestManager.clear(target);
  target.setRequest(request);
  requestManager.track(target, request);

  return target;
}
```

每一个target会持有一个之前的request:`Request previous = target.getRequest();`,之前持有的request与当前的request进行对比,来确定是否复用,如果没有复用则重新为target设置request

```java
// clear target持有的request
requestManager.clear(target);
// 重新给target设置新的requst
target.setRequest(request);
requestManager.track(target, request);
```

接着看`requestManager.track`

```java
  void track(@NonNull Target<?> target, @NonNull Request request) {
    targetTracker.track(target);
    requestTracker.runRequest(request);
  }
  
  public void track(@NonNull Target<?> target) {
    targets.add(target);
  }
  
  

```

target是一个set集合,统一管理目标target,注意glide可以设置优先级

继续看`.runRequest`

```java
public void runRequest(@NonNull Request request) {
  requests.add(request);
  if (!isPaused) {
    request.begin();
  } else {
    request.clear();
    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      Log.v(TAG, "Paused, delaying request");
    }
    pendingRequests.add(request);
  }
}
```

到这里我们可以看到没有OnPause时候request会开始加载,onPause时候会加入缓存队列;到这里我们看到似乎有用到周期;

那么我们再看周期:

#### 6. glide周期绑定流程

```java
@NonNull
public static GlideRequests with(@NonNull FragmentActivity activity) {
  return (GlideRequests) Glide.with(activity);
}
```

```java
@NonNull
public static RequestManager with(@NonNull FragmentActivity activity) {
  return getRetriever(activity).get(activity);
}
```

```java
@NonNull
public RequestManager get(@NonNull FragmentActivity activity) {
  if (Util.isOnBackgroundThread()) {
    return get(activity.getApplicationContext());
  } else {
    assertNotDestroyed(activity);
    FragmentManager fm = activity.getSupportFragmentManager();
    return supportFragmentGet(
        activity, fm, /*parentHint=*/ null, isActivityVisible(activity));
  }
}
```

```java
@NonNull
private RequestManager supportFragmentGet(
    @NonNull Context context,
    @NonNull FragmentManager fm,
    @Nullable Fragment parentHint,
    boolean isParentVisible) {
  SupportRequestManagerFragment current =
      getSupportRequestManagerFragment(fm, parentHint, isParentVisible);
  RequestManager requestManager = current.getRequestManager();
  if (requestManager == null) {
    // TODO(b/27524013): Factor out this Glide.get() call.
    Glide glide = Glide.get(context);
    requestManager =
        factory.build(
            glide, current.getGlideLifecycle(), current.getRequestManagerTreeNode(), context);
    current.setRequestManager(requestManager);
  }
  return requestManager;
}
```

`SupportRequestManagerFragment current =getSupportRequestManagerFragment(fm, parentHint, isParentVisible);`我们设置的碎片已经绑定Activity的周期

我们为我们的碎片设置请求管理:

```java
    requestManager =
        factory.build(
            glide, current.getGlideLifecycle(), current.getRequestManagerTreeNode(), context);
    current.setRequestManager(requestManager);
```

看`requestManager =
factory.build(
glide, current.getGlideLifecycle(), current.getRequestManagerTreeNode(), context);`

我们来看`factory.build`源码

```java
public interface RequestManagerFactory {
  @NonNull
  RequestManager build(
      @NonNull Glide glide,
      @NonNull Lifecycle lifecycle,
      @NonNull RequestManagerTreeNode requestManagerTreeNode,
      @NonNull Context context);
}
```

```java
@Override
@NonNull
public RequestManager build(@NonNull Glide glide, @NonNull Lifecycle lifecycle,
    @NonNull RequestManagerTreeNode treeNode, @NonNull Context context) {
  return new GlideRequests(glide, lifecycle, treeNode, context);
}
```

```java
public GlideRequests(@NonNull Glide glide, @NonNull Lifecycle lifecycle,
    @NonNull RequestManagerTreeNode treeNode, @NonNull Context context) {
  super(glide, lifecycle, treeNode, context);
}
```

```java
public RequestManager(
    @NonNull Glide glide, @NonNull Lifecycle lifecycle,
    @NonNull RequestManagerTreeNode treeNode, @NonNull Context context) {
  this(
      glide,
      lifecycle,
      treeNode,
      new RequestTracker(),
      glide.getConnectivityMonitorFactory(),
      context);
}
```

```java
RequestManager(
    Glide glide,
    Lifecycle lifecycle,
    RequestManagerTreeNode treeNode,
    RequestTracker requestTracker,
    ConnectivityMonitorFactory factory,
    Context context) {
  this.glide = glide;
  this.lifecycle = lifecycle;
  this.treeNode = treeNode;
  this.requestTracker = requestTracker;
  this.context = context;

  connectivityMonitor =
      factory.build(
          context.getApplicationContext(),
          new RequestManagerConnectivityListener(requestTracker));

  // If we're the application level request manager, we may be created on a background thread.
  // In that case we cannot risk synchronously pausing or resuming requests, so we hack around the
  // issue by delaying adding ourselves as a lifecycle listener by posting to the main thread.
  // This should be entirely safe.
  if (Util.isOnBackgroundThread()) {
    mainHandler.post(addSelfToLifecycle);
  } else {
    lifecycle.addListener(this);
  }
  lifecycle.addListener(connectivityMonitor);

  setRequestOptions(glide.getGlideContext().getDefaultRequestOptions());

  glide.registerRequestManager(this);
}
```

我们看到最终我们会走到RequestManager中,而RequestManager实现了LifecycleListener,所以在`lifecycle.addListener(this);`会将接口的实现传入`ActivityFragmentLifecycle`

我们来看ActivityFragmentLifecycle的`addListener`方法:

```java
@Override
public void addListener(@NonNull LifecycleListener listener) {
  lifecycleListeners.add(listener);

  if (isDestroyed) {
    listener.onDestroy();
  } else if (isStarted) {
    listener.onStart();
  } else {
    listener.onStop();
  }
}
```

而在SupportRequestManagerFragment中会实例化LifecycleListener并在其周期进行回调

由于LifecycleListener的注册在RequestManager,而回调在SupportRequestManagerFragment,所以fragment的周期会与RequestManager同步,这样RequestManager就和SupportRequestManagerFragment绑定起来

我们看RequestManager中回调的接口方法

```java
@Override
public void onStart() {
  // 控制请求
  resumeRequests();
  // 加载的target的动画之类
  targetTracker.onStart();
}

/**
 * Lifecycle callback that unregisters for connectivity events (if the
 * android.permission.ACCESS_NETWORK_STATE permission is present) and pauses in progress loads.
 */
@Override
public void onStop() {
  pauseRequests();
  targetTracker.onStop();
}
```

我们看下`resumeRequests`

```java
public void resumeRequests() {
  Util.assertMainThread();
  requestTracker.resumeRequests();
}
```

```java
public void resumeRequests() {
  isPaused = false;
  for (Request request : Util.getSnapshot(requests)) {
    // We don't need to check for cleared here. Any explicit clear by a user will remove the
    // Request from the tracker, so the only way we'd find a cleared request here is if we cleared
    // it. As a result it should be safe for us to resume cleared requests.
    if (!request.isComplete() && !request.isRunning()) {
      request.begin();
    }
  }
  pendingRequests.clear();
}
```

我们看到`RequestTracker`也就是请求的最终入口,我们设置了isPaused,这样请求就和Activity的周期同步了

继续看`targetTracker.onStart()`

```java
@Override
public void onStart() {
  for (Target<?> target : Util.getSnapshot(targets)) {
    target.onStart();
  }
}
```

这个方法是位于TargetTracker中,它里面维护所有target,继续往下看:

```java
@Override
public void onStart() {
  if (animatable != null) {
    animatable.start();
  }
}
```

看到没有,停止了动画

#### 7. 继续看Glide请求

刚刚在看into时候不得不转而看周期,下面接着之前的请求继续看,我们继续从请求入扣着手:

```java
### TargetTracker
public void runRequest(@NonNull Request request) {
  requests.add(request);
  if (!isPaused) {
    request.begin();
  } else {
    request.clear();
    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      Log.v(TAG, "Paused, delaying request");
    }
    pendingRequests.add(request);
  }
}
```

我们看 `request.begin()`,他是singleRequst中的方法:

```java
@Override
public void begin() {
  assertNotCallingCallbacks();
  stateVerifier.throwIfRecycled();
  startTime = LogTime.getLogTime();
  // 如果url为空则请求失败,这个model就是之前load时候传入的url
  if (model == null) {
    if (Util.isValidDimensions(overrideWidth, overrideHeight)) {
      width = overrideWidth;
      height = overrideHeight;
    }
    // Only log at more verbose log levels if the user has set a fallback drawable, because
    // fallback Drawables indicate the user expects null models occasionally.
    int logLevel = getFallbackDrawable() == null ? Log.WARN : Log.DEBUG;
    onLoadFailed(new GlideException("Received null model"), logLevel);
    return;
  }

  if (status == Status.RUNNING) {
    throw new IllegalArgumentException("Cannot restart a running request");
  }

  // If we're restarted after we're complete (usually via something like a notifyDataSetChanged
  // that starts an identical request into the same Target or View), we can simply use the
  // resource and size we retrieved the last time around and skip obtaining a new size, starting a
  // new load etc. This does mean that users who want to restart a load because they expect that
  // the view size has changed will need to explicitly clear the View or Target before starting
  // the new load.
  if (status == Status.COMPLETE) {
    onResourceReady(resource, DataSource.MEMORY_CACHE);
    return;
  }

  // Restarts for requests that are neither complete nor running can be treated as new requests
  // and can run again from the beginning.

  status = Status.WAITING_FOR_SIZE;
  if (Util.isValidDimensions(overrideWidth, overrideHeight)) {
    onSizeReady(overrideWidth, overrideHeight);
  } else {
    target.getSize(this);
  }

  if ((status == Status.RUNNING || status == Status.WAITING_FOR_SIZE)
      && canNotifyStatusChanged()) {
    target.onLoadStarted(getPlaceholderDrawable());
  }
  if (IS_VERBOSE_LOGGABLE) {
    logV("finished run method in " + LogTime.getElapsedMillis(startTime));
  }
}
```

我们看到这段代码中穿插了很多状态我们先看下状态再分析上面的代码:

```java
private enum Status {
  /**
   * Created but not yet running.
   */
  // 等待
  PENDING,
  /**
   * In the process of fetching media.
   */
  // 正在执行
  RUNNING,
  /**
   * Waiting for a callback given to the Target to be called to determine target dimensions.
   */
  // 等待重新确定w/d
  WAITING_FOR_SIZE,
  /**
   * Finished loading media successfully.
   */
  // 加载完成
  COMPLETE,
  /**
   * Failed to load media, may be restarted.
   */
  // 加载失败
  FAILED,
  /**
   * Cleared by the user with a placeholder set, may be restarted.
   */
  // 用户清除了占位符,可能重新启动
  CLEARED,
}
```

我们基于上面代码 关注`COMPLETE`和`WAITING_FOR_SIZE`

我们看到`COMPLETE`时候调用了`onResourceReady`

```java
@Override
public void onResourceReady(Resource<?> resource, DataSource dataSource) {
  stateVerifier.throwIfRecycled();
  loadStatus = null;
  if (resource == null) {
    GlideException exception = new GlideException("Expected to receive a Resource<R> with an "
        + "object of " + transcodeClass + " inside, but instead got null.");
    onLoadFailed(exception);
    return;
  }

  Object received = resource.get();
  if (received == null || !transcodeClass.isAssignableFrom(received.getClass())) {
    releaseResource(resource);
    GlideException exception = new GlideException("Expected to receive an object of "
        + transcodeClass + " but instead" + " got "
        + (received != null ? received.getClass() : "") + "{" + received + "} inside" + " "
        + "Resource{" + resource + "}."
        + (received != null ? "" : " " + "To indicate failure return a null Resource "
        + "object, rather than a Resource object containing null data."));
    onLoadFailed(exception);
    return;
  }

  if (!canSetResource()) {
    releaseResource(resource);
    // We can't put the status to complete before asking canSetResource().
    status = Status.COMPLETE;
    return;
  }

  onResourceReady((Resource<R>) resource, (R) received, dataSource);
  
}
```

主要是请求完成之后完成之后进行资源释放(如引擎释放等),并修改资源状态等 ,这里不做详尽的解释(太多)

#### 8. 加载网络

`Status.WAITING_FOR_SIZE`状态下会调用`onSizeReady`这就是请求网络并裁剪图片的入口:

```java
@Override
public void onSizeReady(int width, int height) {
  stateVerifier.throwIfRecycled();
  if (IS_VERBOSE_LOGGABLE) {
    logV("Got onSizeReady in " + LogTime.getElapsedMillis(startTime));
  }
  if (status != Status.WAITING_FOR_SIZE) {
    return;
  }
  // 这里开始加载,所以Status设置为RUNNING
  status = Status.RUNNING;

  float sizeMultiplier = requestOptions.getSizeMultiplier();
  this.width = maybeApplySizeMultiplier(width, sizeMultiplier);
  this.height = maybeApplySizeMultiplier(height, sizeMultiplier);

  if (IS_VERBOSE_LOGGABLE) {
    logV("finished setup for calling load in " + LogTime.getElapsedMillis(startTime));
  }
  // 引擎请求网络
  loadStatus = engine.load(
      glideContext,
      model,
      requestOptions.getSignature(),
      this.width,
      this.height,
      requestOptions.getResourceClass(),
      transcodeClass,
      priority,
      requestOptions.getDiskCacheStrategy(),
      requestOptions.getTransformations(),
      requestOptions.isTransformationRequired(),
      requestOptions.isScaleOnlyOrNoTransform(),
      requestOptions.getOptions(),
      requestOptions.isMemoryCacheable(),
      requestOptions.getUseUnlimitedSourceGeneratorsPool(),
      requestOptions.getUseAnimationPool(),
      requestOptions.getOnlyRetrieveFromCache(),
      this);

  // This is a hack that's only useful for testing right now where loads complete synchronously
  // even though under any executor running on any thread but the main thread, the load would
  // have completed asynchronously.
  if (status != Status.RUNNING) {
    loadStatus = null;
  }
  if (IS_VERBOSE_LOGGABLE) {
    logV("finished onSizeReady in " + LogTime.getElapsedMillis(startTime));
  }
}
```

`engine.load`是真正开始加载网络,会传入url,图片大小的option,还有一些缓存动画的option

```java
public <R> LoadStatus load(
    GlideContext glideContext,
    Object model,
    Key signature,
    int width,
    int height,
    Class<?> resourceClass,
    Class<R> transcodeClass,
    Priority priority,
    DiskCacheStrategy diskCacheStrategy,
    Map<Class<?>, Transformation<?>> transformations,
    boolean isTransformationRequired,
    boolean isScaleOnlyOrNoTransform,
    Options options,
    boolean isMemoryCacheable,
    boolean useUnlimitedSourceExecutorPool,
    boolean useAnimationPool,
    boolean onlyRetrieveFromCache,
    ResourceCallback cb) {
  Util.assertMainThread();
  long startTime = VERBOSE_IS_LOGGABLE ? LogTime.getLogTime() : 0;
	
  // 生成key
  EngineKey key = keyFactory.buildKey(model, signature, width, height, transformations,
      resourceClass, transcodeClass, options);
	
  // 从Active缓存加载
  EngineResource<?> active = loadFromActiveResources(key, isMemoryCacheable);
  if (active != null) {
    cb.onResourceReady(active, DataSource.MEMORY_CACHE);
    if (VERBOSE_IS_LOGGABLE) {
      logWithTimeAndKey("Loaded resource from active resources", startTime, key);
    }
    return null;
  }
	// 从lruCache
  EngineResource<?> cached = loadFromCache(key, isMemoryCacheable);
  if (cached != null) {
    cb.onResourceReady(cached, DataSource.MEMORY_CACHE);
    if (VERBOSE_IS_LOGGABLE) {
      logWithTimeAndKey("Loaded resource from cache", startTime, key);
    }
    return null;
  }

  EngineJob<?> current = jobs.get(key, onlyRetrieveFromCache);
  if (current != null) {
    current.addCallback(cb);
    if (VERBOSE_IS_LOGGABLE) {
      logWithTimeAndKey("Added to existing load", startTime, key);
    }
    return new LoadStatus(cb, current);
  }

  EngineJob<R> engineJob =
      engineJobFactory.build(
          key,
          isMemoryCacheable,
          useUnlimitedSourceExecutorPool,
          useAnimationPool,
          onlyRetrieveFromCache);

  DecodeJob<R> decodeJob =
      decodeJobFactory.build(
          glideContext,
          model,
          key,
          signature,
          width,
          height,
          resourceClass,
          transcodeClass,
          priority,
          diskCacheStrategy,
          transformations,
          isTransformationRequired,
          isScaleOnlyOrNoTransform,
          onlyRetrieveFromCache,
          options,
          engineJob);

  jobs.put(key, engineJob);
	// 网络拉取
  engineJob.addCallback(cb);
  engineJob.start(decodeJob);

  if (VERBOSE_IS_LOGGABLE) {
    logWithTimeAndKey("Started new load", startTime, key);
  }
  return new LoadStatus(cb, engineJob);
}
```

第一级缓存  Active Cache

`EngineResource<?> active = loadFromActiveResources(key, isMemoryCacheable);`

第二级缓存 LruCache

  `EngineResource<?> cached = loadFromCache(key, isMemoryCacheable);`

第三个网络拉取:

```
engineJob.addCallback(cb);
engineJob.start(decodeJob);
```

所以我们可以使用glide监听:

```java
 SimpleTarget<Drawable> into = GlideApp.with(this)
                .load(url)
                .placeholder(R.mipmap.ic_launcher_round)
                //开始请求
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        Log.i("glide_life", "onResourceReady");
                        mIv.setImageDrawable(resource);
                    }

                    @Override
                    public void setRequest(@Nullable Request request) {
                        super.setRequest(request);
                        Log.i("glide_life", "setRequest");

                    }

                     .....省略一些方法
                });

```

继续看`engineJob.start(decodeJob);`

```java

public void start(DecodeJob<R> decodeJob) {
  this.decodeJob = decodeJob;
  GlideExecutor executor = decodeJob.willDecodeFromCache()
      ? diskCacheExecutor
      : getActiveSourceExecutor();
  // 线程池请求
  executor.execute(decodeJob);
}
```

`executor.execute(decodeJob);`

```java
@Override
public void execute(@NonNull Runnable command) {
  delegate.execute(command);
}
```

这是一个线程池最后走其run方法:

```java
@Override
public void run() {
  // This should be much more fine grained, but since Java's thread pool implementation silently
  // swallows all otherwise fatal exceptions, this will at least make it obvious to developers
  // that something is failing.
  GlideTrace.beginSectionFormat("DecodeJob#run(model=%s)", model);
  // Methods in the try statement can invalidate currentFetcher, so set a local variable here to
  // ensure that the fetcher is cleaned up either way.
  DataFetcher<?> localFetcher = currentFetcher;
  try {
    if (isCancelled) {
      // 如果周期cancel就取消网络 返回fail
      notifyFailed();
      return;
    }
    // 正常请求网络
    runWrapped();
  } catch (Throwable t) {
    // Catch Throwable and not Exception to handle OOMs. Throwables are swallowed by our
    // usage of .submit() in GlideExecutor so we're not silently hiding crashes by doing this. We
    // are however ensuring that our callbacks are always notified when a load fails. Without this
    // notification, uncaught throwables never notify the corresponding callbacks, which can cause
    // loads to silently hang forever, a case that's especially bad for users using Futures on
    // background threads.
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "DecodeJob threw unexpectedly"
          + ", isCancelled: " + isCancelled
          + ", stage: " + stage, t);
    }
    // When we're encoding we've already notified our callback and it isn't safe to do so again.
    if (stage != Stage.ENCODE) {
      throwables.add(t);
      // 异常状态返回fail
      notifyFailed();
    }
    if (!isCancelled) {
      throw t;
    }
  } finally {
    // Keeping track of the fetcher here and calling cleanup is excessively paranoid, we call
    // close in all cases anyway.
    if (localFetcher != null) {
      localFetcher.cleanup();
    }
    GlideTrace.endSection();
  }
}
```

周期cancel&异常 — `notifyFailed()`

正常请求 — `runWrapped();`

我们继续看`runWrapped`

```jaava
private void runWrapped() {
  switch (runReason) {
    case INITIALIZE:
      stage = getNextStage(Stage.INITIALIZE);
      currentGenerator = getNextGenerator();
      runGenerators();
      break;
    case SWITCH_TO_SOURCE_SERVICE:
      runGenerators();
      break;
    case DECODE_DATA:
      decodeFromRetrievedData();
      break;
    default:
      throw new IllegalStateException("Unrecognized run reason: " + runReason);
  }
}
```

`getNextStage`获取加载状态

`getNextGenerator()`获取图片加载器

```java
private DataFetcherGenerator getNextGenerator() {
  switch (stage) {
    case RESOURCE_CACHE:
      return new ResourceCacheGenerator(decodeHelper, this);
    case DATA_CACHE:
      return new DataCacheGenerator(decodeHelper, this);
    case SOURCE:
      return new SourceGenerator(decodeHelper, this);
    case FINISHED:
      return null;
    default:
      throw new IllegalStateException("Unrecognized stage: " + stage);
  }
}
```

他有3中加载器:

`ResourceCacheGenerator`,`DataCacheGenerator`和`SourceGenerator`

我们接着看`runGenerators()`

```java
private void runGenerators() {
  currentThread = Thread.currentThread();
  startFetchTime = LogTime.getLogTime();
  boolean isStarted = false;
  while (!isCancelled && currentGenerator != null
      && !(isStarted = currentGenerator.startNext())) {
    stage = getNextStage(stage);
    currentGenerator = getNextGenerator();

    if (stage == Stage.SOURCE) {
      reschedule();
      return;
    }
  }
  // We've run out of stages and generators, give up.
  if ((stage == Stage.FINISHED || isCancelled) && !isStarted) {
    notifyFailed();
  }

  // Otherwise a generator started a new load and we expect to be called back in
  // onDataFetcherReady.
}
```

加载的逻辑是`currentGenerator.startNext()`

以source为例:

```java
@Override
public boolean startNext() {
  if (dataToCache != null) {
    Object data = dataToCache;
    dataToCache = null;
    cacheData(data);
  }

  if (sourceCacheGenerator != null && sourceCacheGenerator.startNext()) {
    return true;
  }
  sourceCacheGenerator = null;

  loadData = null;
  boolean started = false;
  while (!started && hasNextModelLoader()) {
    loadData = helper.getLoadData().get(loadDataListIndex++);
    if (loadData != null
        && (helper.getDiskCacheStrategy().isDataCacheable(loadData.fetcher.getDataSource())
        || helper.hasLoadPath(loadData.fetcher.getDataClass()))) {
      started = true;
      loadData.fetcher.loadData(helper.getPriority(), this);
    }
  }
  return started;
}
```