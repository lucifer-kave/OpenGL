### 伴鱼:

#### 1. 为什么intent只能传1M

- 通过手写open，mmap初始化Binder服务的限制是4MB

- 通过ProcessState初始化Binder服务的限制是1MB-8KB

 普通的应用是由Zygote孵化而来的用户进程，所映射的Binder内存大小是不到1M的，准确说是 110241024) - (4096 *2) ：这个限制定义在frameworks/native/libs/binder/processState.cpp类中，如果传输说句超过这个大小，系统就会报错，因为Binder本身就是为了进程间频繁而灵活的通信所设计的，并不是为了拷贝大数据而使用的，所以当传递大的数据时会出现上述的错误

- java.lang.RuntimeException: android.os.TransactionTooLargeException: data parcel size 4194612 bytes

Intent 传输数据的机制中，用到了 Binder。Binder内存大小是不到1M的,Binder 是为了进程间频繁而灵活的通信所设计的。

#### 2. 为什么事件分发中收到`ACTION_DOWN`,还会收到MOVE 和 UP时间

<https://www.jianshu.com/p/5951ebdd2a7e>

- 判断是否需要拦截 —> 主要是根据 onInterceptTouchEvent 方法的返回值来决定是否拦截；
- 在 DOWN 事件中将 touch 事件分发给子 View —> 这一过程如果有子 View 捕获消费了 touch 事件，会对 mFirstTouchTarget 进行赋值；
- dispatchTouchEvent 的最后一步，DOWN、MOVE、UP 事件都会根据 mFirstTouchTarget 是否为 null，决定是自己处理 touch 事件，还是分发给子 View。
- DOWN 事件是事件序列的起点；决定后续事件由谁来消费处理；
- mFirstTouchTarget 的作用：记录捕获消费 touch 事件的 View，是一个链表结构；
- CANCEL 事件的触发场景：当父视图先不拦截，然后在 MOVE 事件中重新拦截，此时子 View 会接收到一个 CANCEL 事件。
- 如果一个事件最后所有的 View 都不处理的话，最终回到 Activity 的 onTouchEvent 方法里面来。

#### 3. AIDL中的oneway

#### 4.jetpack

livedata — 传入lifecycleowner进行周期当定,可以监听observer,然后进行view更新,优于mvp,不需要人工设置接口

viewmodel— 存储状态进行恢复  

#### 5. 多dex优化

<https://mp.weixin.qq.com/s/gA758L13pc0UjCwvNaT3aQ>





