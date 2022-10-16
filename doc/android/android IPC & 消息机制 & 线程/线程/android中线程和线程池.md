#### 1. 主线程和子线程

- 主线程:是指进程所拥有的线程,在java中默认情况下一个进程只有一个线程,这个线程就是主线程,这个线程就是主线程
  - 主线程主要处理界面交互相关的逻辑,因为用户随时会和界面发生交互,因此主线程在任何时候必须拥有较高响应速度,负责会卡顿
- 子线程:为了在不影响主线程响应的情况下处理耗时任务,会使用子线程;子线程也叫工作线程,除了主线程意外的线程都是子线程

android中沿用了java的线程模型,其中的线程也分为主线程和子线程,其中:

- 主线程:UI线程;主线程的作用是运行四大组件以及处理他们和用户的交互
- 子线程:执行耗时任务,比如网络请求,I/O操作等;

#### 2. Android中的线程形态

##### 2.1AsyncTask

AsyncTask是一种轻量级的一部任务类,他可以在线程池中执行后台任务,然后把执行进度和最终结果传递给主线程,并且在主线程中更新UI

AsyncTask封装了Thread和Handler,通过AsyncTask可以更加方便的执行后台任务以及主线程中访问UI,但是AsyncTask并不是适合进行特别耗时的后台任务,对于特别耗时的任务来说,建议使用线程池



```java
private class  DownLoadFilesTask extends AsyncTask<URL,Integer,Long>{

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // 执行异步方法之前可以执行一部分初始化操作
        Log.i(TAG,"onPreExecute: "+ Thread.currentThread().getName());

    }

    @Override
    protected Long doInBackground(URL... urls) {
      	// 异步线程
        Log.i(TAG,"doInBackground: "+ Thread.currentThread().getName());
        int count = urls.length;
        long totalSize = 0;
        for (int i = 0; i < count; i++) {
            // totalSize += Downloader.
            publishProgress((int)(i/count)*1000);
        }
        return totalSize;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
      	// 主线程更新progress
        super.onProgressUpdate(values);
        Log.i(TAG,"onProgressUpdate: "+ Thread.currentThread().getName() + ":" + values[0]+"");

    }


    @Override
    protected void onPostExecute(Long aLong) {
      	// 主线程更新ui
        super.onPostExecute(aLong);
        Log.i(TAG,"onPostExecute: "+ Thread.currentThread().getName() + ":" + aLong);
    }
}
```

介绍下AsyncTask的四个核心方法

- `onPreExecute`:
  - 在主线程中执行,在异步执行之前,词方法会被调用,一般是一些初始化的操作
- `doInBackground`:
  - 在线程池中执行,词方法用于执行异步任务,`parmas`参数表示输入参数,在此方法中可以通publishProgress方法来更新任务的进度,publishProgress方法会调用onProgressUpdate方法;其次词方法需要返回计算结果给onPostExecute
- `onProgressUpdate`:
  - 主线程执行,``doInBackground`会通过调用publishProgress来调用改方法更新进度
- `onPostExecute`:
  - 同样在主线程执行,在异步任务执行之后被调用,其中result是后台任务的返回值,也就是`doInBackground`的返回值

- `onCancelled`:
  - 同样在主线程执行,在异步任务取消时被调用

**AsyncTask使用过程中需要注意一些限制**

- 1. AsyncTask类必须在主线程加载,android4.1及以上版本系统自动完成
  2. AsyncTask对象必须在主线程中创建
  3. execute方法必须在UI线程中创建
  4. 不要再 程序中直接调用`onPreExecute()`,`onPostExecute()`,`doInBackground()`和`onProgressUpdate()`
  5. 一个AsyncTask对象只能执行一次,即只能调用一次`execute`方法,否则回报运行时异常

  6. 在android 1.6之前,AsyncTask是串行执行任务的,Android 1.6的时候AsyncTask开始采用线程池里处理并行任务,但是从android 3.0开始为了避免AsyncTask锁带来的并发错误,AsyncTask又采用一个线程来串行执行任务,尽管如此,在Android3.0以及后序的版本中,我们任然可以通过AsyncTask的`executeOnExecutor`方法来执行任务



##### 2.2 AsyncTask工作原理

###### 2.2.1 AsyncTask调用入口 `.execute`

```java
// 可以看到入口函数传入的是doInBackground的入参
@MainThread
public final AsyncTask<Params, Progress, Result> execute(Params... params) {
  	// sDefaultExecutor就是AsyncTask的线程池
    return executeOnExecutor(sDefaultExecutor, params);
}
```

```java
@MainThread
public final AsyncTask<Params, Progress, Result> executeOnExecutor(Executor exec,
        Params... params) {
    if (mStatus != Status.PENDING) {
        switch (mStatus) {
            case RUNNING:
                throw new IllegalStateException("Cannot execute task:"
                        + " the task is already running.");
            case FINISHED:
                throw new IllegalStateException("Cannot execute task:"
                        + " the task has already been executed "
                        + "(a task can be executed only once)");
        }
    }

    mStatus = Status.RUNNING;
		
  	// 优先执行
    onPreExecute();

    mWorker.mParams = params;
    exec.execute(mFuture);

    return this;
}
```

- `sDefaultExecutor`是一个串行的线程池,一个进程中的所有AsyncTask全部在这个串行的线程池中排队执行
- 在的`executeOnExecutor`中,AsyncTask中`onPreExecute();`最先执行

- 线程池开始执行`execute`

###### 2.2.2  线程池 `sDefaultExecutor`

```java

public static final Executor SERIAL_EXECUTOR = new SerialExecutor();
private static volatile Executor sDefaultExecutor = SERIAL_EXECUTOR;
private static class SerialExecutor implements Executor {
    final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
    Runnable mActive;

    public synchronized void execute(final Runnable r) {
      	// 将传入的AsyncTask加入队列,这里的Runnable就是mFuture-- 将任务加载到第一个
        mTasks.offer(new Runnable() {
            public void run() {
                try {
                    r.run();
                } finally {
                    scheduleNext();
                }
            }
        });
      	// 判断有没有正在活动的AsyncTask任务
        if (mActive == null) {
            scheduleNext();
        }
    }

    protected synchronized void scheduleNext() {
      	// 去除ArrayDeque中第一个元素执行
        if ((mActive = mTasks.poll()) != null) {
            THREAD_POOL_EXECUTOR.execute(mActive);
        }
    }
}
```

从 `SerialExecutor`的源码可以分析到AsyncTask的排队策略:

1. AsyncTask构造中会将parmas进行封装

   ```java
   
    public AsyncTask(@Nullable Looper callbackLooper) {
           mHandler = callbackLooper == null || callbackLooper == Looper.getMainLooper()
               ? getMainHandler()
               : new Handler(callbackLooper);
   
           mWorker = new WorkerRunnable<Params, Result>() {
               public Result call() throws Exception {
                   mTaskInvoked.set(true);
                   Result result = null;
                   try {
                       Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                       //noinspection unchecked
                       result = doInBackground(mParams);
                       Binder.flushPendingCommands();
                   } catch (Throwable tr) {
                       mCancelled.set(true);
                       throw tr;
                   } finally {
                       postResult(result);
                   }
                   return result;
               }
           };
   
           mFuture = new FutureTask<Result>(mWorker) {
               @Override
               protected void done() {
                   try {
                       postResultIfNotInvoked(get());
                   } catch (InterruptedException e) {
                       android.util.Log.w(LOG_TAG, e);
                   } catch (ExecutionException e) {
                       throw new RuntimeException("An error occurred while executing doInBackground()",
                               e.getCause());
                   } catch (CancellationException e) {
                       postResultIfNotInvoked(null);
                   }
               }
           };
       }
   ```

   1. 其中`mWork` `implements Callable<Result> `他会将paramas传入,最后返回result,`mFuture`继承自runnable接口
   2. 在  `exec.execute(mFuture);`开始执行线程池的操作
   3. 然后根据上面线程池的代码,`FutureTask`会先加入线程池的`TaskQueue`,然后判断此刻是否有活跃的AsyncTask任务,没有就执行队列中的下一条Task

**到这里我们可以看到,AsyncTask默认是串行的**

由于是线程池调用,且FeatureTask是继承自Runnable,所以start线程一定会调用`FeatureTask`的run方法:

所以接着我们看FeatureTask和WorkerRunnable

######2.2.3 FeatureTask和WorkerRunnable

FeatureTask源码:

```java
// 这个callable就是mWorker
public FutureTask(Callable<V> callable) {
    if (callable == null)
        throw new NullPointerException();
    this.callable = callable;
    this.state = NEW;       // ensure visibility of callable
}
```

重点看其run方法,因为在线程池中运行会回调他的run方法



```java
public void run() {
    if (state != NEW ||
        !U.compareAndSwapObject(this, RUNNER, null, Thread.currentThread()))
        return;
    try {
        Callable<V> c = callable;
        if (c != null && state == NEW) {
            V result;
            boolean ran;
            try {
              	// 1.会调mWorker的call方法
                result = c.call();
                ran = true;
            } catch (Throwable ex) {
                result = null;
                ran = false;
                setException(ex);
            }
            if (ran)
              	// 设置返回的result
                set(result);
        }
    } finally {
        // runner must be non-null until state is settled to
        // prevent concurrent calls to run()
        runner = null;
        // state must be re-read after nulling runner to prevent
        // leaked interrupts
        int s = state;
        if (s >= INTERRUPTING)
            handlePossibleCancellationInterrupt(s);
    }
}
```

很显然run方法会回调mWorker的call方法

那么我们看下AsyncTask中`mWorker`的具体实现:

```java
public AsyncTask(@Nullable Looper callbackLooper) {
    mHandler = callbackLooper == null || callbackLooper == Looper.getMainLooper()
        ? getMainHandler()
        : new Handler(callbackLooper);

    mWorker = new WorkerRunnable<Params, Result>() {
        public Result call() throws Exception {
           // 这里设置为true,表示当前任务已经被掉又怕你个 
            mTaskInvoked.set(true);
            Result result = null;
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                //noinspection unchecked
                result = doInBackground(mParams);
                Binder.flushPendingCommands();
            } catch (Throwable tr) {
                mCancelled.set(true);
                throw tr;
            } finally {
                postResult(result);
            }
            return result;
        }
    };

    mFuture = new FutureTask<Result>(mWorker) {...};
}
```

也就是说线程池中执行的就是mWorker的call代码,核心代码就是AsyncTask的`result = doInBackground(mParams);`

在上面FeatureTask的run逻辑代码里,执行mWorker的call,最后会返回一个result(也就是`doInBackground`中的返回值),然后`set(result);`

###### 2.2.4 doInBackground之后set其返回值

```java
protected void set(V v) {
    if (U.compareAndSwapInt(this, STATE, NEW, COMPLETING)) {
        outcome = v;
        U.putOrderedInt(this, STATE, NORMAL); // final state
        finishCompletion();
    }
}
```

```java
private void finishCompletion() {
    // assert state > COMPLETING;
    for (WaitNode q; (q = waiters) != null;) {
        if (U.compareAndSwapObject(this, WAITERS, q, null)) {
            for (;;) {
                Thread t = q.thread;
                if (t != null) {
                    q.thread = null;
                    LockSupport.unpark(t);
                }
                WaitNode next = q.next;
                if (next == null)
                    break;
                q.next = null; // unlink to help gc
                q = next;
            }
            break;
        }
    }

    done();

    callable = null;        // to reduce footprint
}
```

最后在`finishCompletion`中末尾调用FeatureTask的done方法,这个方法前面说过实在AsyncTask中实现的:

```java
mFuture = new FutureTask<Result>(mWorker) {
    @Override
    protected void done() {
        try {
          	//核心代码
            postResultIfNotInvoked(get());
        } catch (InterruptedException e) {
            android.util.Log.w(LOG_TAG, e);
        } catch (ExecutionException e) {
            throw new RuntimeException("An error occurred while executing doInBackground()",
                    e.getCause());
        } catch (CancellationException e) {
            postResultIfNotInvoked(null);
        }
    }
};
```

继续看`postResultIfNotInvoked(get());` 

```java
private void postResultIfNotInvoked(Result result) {
  	// 判断当前任务是否被调用
    final boolean wasTaskInvoked = mTaskInvoked.get();
    if (!wasTaskInvoked) {
        postResult(result);
    }
}
```

最终回到这里:

```java
private Result postResult(Result result) {
    @SuppressWarnings("unchecked")
    Message message = getHandler().obtainMessage(MESSAGE_POST_RESULT,
            new AsyncTaskResult<Result>(this, result));
    message.sendToTarget();
    return result;
}
```

这里其实可以看出端倪:

`doInBackground`返回的result应该要切换线程给`onPostExecute(result)`

只是猜想,继续往下看:

```java
private static class InternalHandler extends Handler {
    public InternalHandler(Looper looper) {
        super(looper);
    }

    @SuppressWarnings({"unchecked", "RawUseOfParameterizedType"})
    @Override
    public void handleMessage(Message msg) {
        AsyncTaskResult<?> result = (AsyncTaskResult<?>) msg.obj;
        switch (msg.what) {
            case MESSAGE_POST_RESULT:
                // There is only one result
            		// 调用`onPostExecute`
                result.mTask.finish(result.mData[0]);
                break;
            case MESSAGE_POST_PROGRESS:
            		// 调用 onProgressUpdate
                result.mTask.onProgressUpdate(result.mData);
                break;
        }
    }
}
```

不错就是切换线程的逻辑

######2.2.5 `result.mTask.finish(result.mData[0]);`

这里的`result.mTask`就是AsyncTask,所以这里调用AsyncTask的`finish`

```java
private void finish(Result result) {
    if (isCancelled()) {
        onCancelled(result);
    } else {
        onPostExecute(result);
    }
    mStatus = Status.FINISHED;
}
```

#### 

##### 2.3 HandlerThread

使用

```java
// 步骤1：创建HandlerThread实例对象
// 传入参数 = 线程名字，作用 = 标记该线程
   HandlerThread mHandlerThread = new HandlerThread("handlerThread");

// 步骤2：启动线程
   mHandlerThread.start();

// 步骤3：创建工作线程Handler & 复写handleMessage（）
// 作用：关联HandlerThread的Looper对象、实现消息处理操作 & 与其他线程进行通信
// 注：消息处理操作（HandlerMessage（））的执行线程 = mHandlerThread所创建的工作线程中执行
  Handler workHandler = new Handler( handlerThread.getLooper() ) {
            @Override
            public boolean handleMessage(Message msg) {
                ...//消息处理
                return true;
            }
        });

// 步骤4：使用工作线程Handler向工作线程的消息队列发送消息
// 在工作线程中，当消息循环时取出对应消息 & 在工作线程执行相关操作
  // a. 定义要发送的消息
  Message msg = Message.obtain();
  msg.what = 2; //消息的标识
  msg.obj = "B"; // 消息的存放
  // b. 通过Handler发送消息到其绑定的消息队列
  workHandler.sendMessage(msg);

// 步骤5：结束线程，即停止线程的消息循环
  mHandlerThread.quit();
```



##### 2.4 IntentService

IntentService是一种特殊的Service,它继承自Service,并且他是一个抽象类,因此必须创建他的子类才能使用IntentService

IntentService可用于执行后台耗时任务,当任务执行后他会自动停止,同时由于IntentService是服务的原因,导致他的优先级要高于单纯的线程

所以IntentService比较适合执行一些高优先级的后台任务,因为他们优先级高不容气被系统杀死

###### 2.4.1 IntentService.onCreate

在onCreate中维护了一个 HandlerThread和一个Handler(同一个Looper)

```java
@Override
public void onCreate() {
    // TODO: It would be nice to have an option to hold a partial wakelock
    // during processing, and to have a static startService(Context, Intent)
    // method that would launch the service & hand off a wakelock.

    super.onCreate();
    HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
    thread.start();

    mServiceLooper = thread.getLooper();
    mServiceHandler = new ServiceHandler(mServiceLooper);
}
```

IntentService被第一次启动的时候,他的onCreate方法会被调用,会用同一个looper创建HandlerThread和Handler,这样通过mServiceHandler发送的消息最终会在HandlerThread中执行

每次启动IntentService,他的onStartCommmand会处理每个后台的intent

###### 2.4.2 IntentService.onStartCommand

```java
@Override
public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
    onStart(intent, startId);
    return mRedelivery ? START_REDELIVER_INTENT : START_NOT_STICKY;
}
```

接着 调用onStart

###### 2.4.3  IntentService.onStart

```java
@Override
public void onStart(@Nullable Intent intent, int startId) {
    Message msg = mServiceHandler.obtainMessage();
    msg.arg1 = startId;
    msg.obj = intent;
    mServiceHandler.sendMessage(msg);
}
```

接着发送消息,在其内部的handler处理会调

###### 2.4.4  IntentService.ServiceHandler

```java
private final class ServiceHandler extends Handler {
    public ServiceHandler(Looper looper) {
        super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
        onHandleIntent((Intent)msg.obj);
        stopSelf(msg.arg1);
    }
}
```

- handler会将消息发到这里进行处理,通过`onHandleIntent`去完成实现;IntentService中onHandleIntent是一个抽象类,强制需要实现
-  stopSelf(msg.arg1);不会立刻停止,只有当` onHandleIntent;`方法执行完最后一个task时候才会停止
- 由于每次执行一个后台的任务就必须启动一次IntentService,而IntentService内部则通过消息的方式向HandlerThread请求执行任务,handler中的Looper是顺序处理消息的,这就意味者IntentService也是顺序执行后台任务的

#### 3. Android中的线程池

首先先说优点:

- 重用线程池中的线程,避免因为线程的创建和销毁带来的性能的消耗
- 能有效的控制线程池的最大并发数,避免大量的线程之间因互相抢占系统资源而导致的阻塞现象

- 能够对线程进行简单的管理,并提供定时执行以及制定时间间隔循环执行等功能

##### 3.1 ThreadPoolExecutor

ThreadPoolExecutor是线程池的真正实现,他的构造方法提供了一系列的参数来配置线程池:

```java
public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          ThreadFactory threadFactory) {
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
         threadFactory, defaultHandler);
}
```

- corePoolSize

  - 线程池的核心线程数,默认情况下,核心线程会在线程中一直存活,即便他处于闲置状态

  - 如果将ThreadPoolExecutor的

    ```java
    public boolean allowsCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }
    ```

    上面方法设置为true,那么闲置的核心线程在等待新任务到来时会有超时策略,这个时间间隔由`keepAliveTime`决定,当等待时间超过`keepAliveTime`所指定的时长时候,核心线程就会被终止

- maximumPoolSize

  - 线程池所能容纳的最大线程数,当线程数达到这个数值时候,后序的新任务将会被阻塞

- keepAliveTime

  - allowsCoreThreadTimeOut为false,只适用于非核心线程,为true,同时作用于核心线程和非核心线程

- unit

  - 用于制定`keepAliveTime`的时间单位

    ```java
    public enum TimeUnit {
     		// 纳秒
        NANOSECONDS 
        // 微秒
        MICROSECONDS 
    		// 毫秒
        MILLISECONDS 
    		// 秒
        SECONDS 
    		// 分
        MINUTES
    		// 时
        HOURS 
    		// 天
        DAYS
    ```

- workQueue

  - 线程池中的任务队列,通过线程池的execute方法提交的Runnable对象会存储在这里

- threadFactory

  - 线程工厂,为线程池提供创建新线程的功能,`threadFactory`,他只有一个方法

  ```java
    public Thread newThread(Runnable r) {
              return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
          }
  ```

- RejectedExecutionHandler
  - 这是一个不常用的参数,当线程池无法执行新任务的时候,可能是由于任务队列已满或者无法成功执行任务,这个参数会调用handler抛出一个Exception

**ThreadPoolExecutor执行任务时遵循的规则:**

1. 如果线程池中线程数量没有达到核心线程的数量,那么会直接启动一个核心线程来执行任务
2. 如果线程池中的线程数量已经达到或者超过核心线程的数量,那么任务会被插入到任务队列中排队等待
3. 如果步骤2中无法将任务插入到任务队列中,这往往是由于任务队列已经满了,这个时候如果线程数量未达到线程池规定的最大值,那么会立即启动一个非核心线程来执行任务
4. 如果步骤3中线程数已经达到线程池规定的最大值,那么就拒绝执行此任务,即调用RejectedExecutionHandler抛出异常



我们看简单看下 AsyncTask中线程池的配置:

```java
// 核心线程数最大是4  Math.max(2, Math.min(CPU_COUNT - 1, 4));
private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
// 线程池最大线程数是 CPU核心数*2 + 1
private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
// 非核心线程超时是30s
private static final int KEEP_ALIVE_SECONDS = 30;

private static final ThreadFactory sThreadFactory = new ThreadFactory() {
    private final AtomicInteger mCount = new AtomicInteger(1);

    public Thread newThread(Runnable r) {
        return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
    }
};

private static final BlockingQueue<Runnable> sPoolWorkQueue =
        new LinkedBlockingQueue<Runnable>(128);



static {
    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
            sPoolWorkQueue, sThreadFactory);
    threadPoolExecutor.allowCoreThreadTimeOut(true);
    THREAD_POOL_EXECUTOR = threadPoolExecutor;
}
```

##### 3.2 线程池的分类

- FixedThreadPool:	

  - 通过Execute的newFixedThreadPool进行创建,他是一种线程数量固定的线程池,当线程处于空闲状态时候,他们并不会回收,除非线程池关闭了;当所有的线程都出于活动状态时候,新任务就会出于等待状态,直到有线程空闲出来;
  - 只有核心线程并且这些线程不会被回收,所以可以加速外界的请求

  ```java
  public static ExecutorService newFixedThreadPool(int nThreads) {
      return new ThreadPoolExecutor(nThreads, nThreads,
                                    0L, TimeUnit.MILLISECONDS,
                                    new LinkedBlockingQueue<Runnable>());
  }
  ```

- CachedThreadPool:
  - 是一种线程数量不定的线程池,他只有非核心线程,并且其最大线程数为Inreger.MAX_VALUE;也就是说最大线程数可以任意大;
  - 当线程池中线程都出于活动状态时,线程池会创建新的线程来处理新任务,否则就会利用空闲的线程来处理新任务,线程池中空闲线程是有超时机制的,这个超时时长是60s,超过60s闲置线程就会被回收

- ScheduledThreadPool:
  - 核心线程数固定,非核心线程数是没有限制的,并且当非核心线程数闲置时会被立即回收
- SingleThreadExecutor
  - 只有一个核心线程,他确保所有任务在同一个线程中按顺序执行

