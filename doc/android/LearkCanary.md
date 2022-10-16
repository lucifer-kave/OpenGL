1，用ActivityLifecycleCallbacks接口来检测Activity生命周期
 2，WeakReference + ReferenceQueue 来监听对象回收情况
 3，Apolication中可通过processName判断是否是任务执行进程
 4，MessageQueue中加入一个a来得到主线程空闲回调
 5，LeakCanary检测只针对Activiy里的相关对象。其他类无法使用，还得用MAT原始方法





1，用ActivityLifecycleCallbacks接口来检测Activity生命周期
 2，WeakReference + ReferenceQueue 来监听对象回收情况
 3，Apolication中可通过processName判断是否是任务执行进程
 4，MessageQueue中加入一个IdleHandler来得到主线程空闲回调
 5，LeakCanary检测只针对Activiy里的相关对象。其他类无法使用，还得用MAT原始方法





