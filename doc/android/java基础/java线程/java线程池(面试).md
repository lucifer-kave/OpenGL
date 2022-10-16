#### 1. Java线程池分类

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

  **优点:**

  **1.可以控制线程最大并发数(同时执行的线程数)**

  **2.超出线程会在对类中等待**

  **3.对类无限大**

  

  

- CachedThreadPool:

  - 是一种线程数量不定的线程池,他只有非核心线程,并且其最大线程数为Inreger.MAX_VALUE;也就是说最大线程数可以任意大;
  - 当线程池中线程都出于活动状态时,线程池会创建新的线程来处理新任务,否则就会利用空闲的线程来处理新任务,线程池中空闲线程是有超时机制的,这个超时时长是60s,超过60s闲置线程就会被回收

  **优点:**

  **1.线程无限制**

  **2.有空闲线程则进行复用**

  **3.一定程度上减少频繁创建/销毁线程,减少系统开销**

  

  

- ScheduledThreadPool:

  - 核心线程数固定,非核心线程数是没有限制的,并且当非核心线程数闲置时会被立即回收

  **优点:**

  **1.线程无限制**

  **队列无限大**

  **2.有空闲线程则进行复用**

  **3.一定程度上减少频繁创建/销毁线程,减少系统开销**

  

- SingleThreadExecutor

  - 只有一个核心线程,他确保所有任务在同一个线程中按顺序执行

  **优点:**

  **1.有且仅有一个工作线程执行任务**

  **2.所有任务按照指定顺序执行,即遵循队列的入队出队规则**



#### 2.线程池的优点:

- 重用线程池中的线程,避免因为线程的创建和销毁带来的性能的消耗
- 能有效的控制线程池的最大并发数,避免大量的线程之间因互相抢占系统资源而导致的阻塞现象
- 能够对线程进行简单的管理,并提供定时执行以及制定时间间隔循环执行等功能



#### 3. 线程池原理 

同事使用了**BlockingQueue**和**HashSet**集合构成,从任务提交的流程来看,对于使用线程池的外部来说,线程池的机制是这样的

1. 如果正在运行线程数<`corePoolSize`,马上创建核心线程执行该task,不排队等候
2. 如果正在运行的线程数>=`corePoolSize`,把该task加入阻塞队列
3. 如果队列已满 && 正在运行的线程数 < `maximumPoolSize`,创建新的非核心线程执行该task
4. 如果队列已满 && 正在运行的线程数 >= `maximumPoolSize`,线程池调用handler的reject方法拒绝本次提交



#### 4. 线程池的复用

需要深入源码 去看`addWorker()--> runWorker()`

- firstTask:这是指定的第一个tunnable可执行任务,他会在worker这个功过线程中运行任务run.并且置空表示这个任务已经被执行
- getTask是在runWorker中的一个死循环,此时所有worker均已经启动,如果处于闲置状态,就会通过`getTask`从workQueue中去除task进行run



其实就是任务并不只是执行创建时制定的firstTask第一任务,还会从任务队列中通过`getTask()`方法自己主动去取任务执行,而且是有/无时间限定的阻塞等待,保证线程的存活

#### 5. 信号量

semaphore可用于进程间同步也可以用于同一进程间线程同步



可以用来保证两个或者多个关键代码段不被并发调用,在进入一个关键代码段之前,线程必须获取一个信号量;一旦该关键代码段完成了,那么线程必须释放信号量.其他想进入该代码段的线程必须等待知道第一个线程释放信号量



#### 6 线程池有哪几种工作队列

- SynchronousQueue

  - 接到任务之后会直接提交给线程处理,而不保留他,如果所有线程都在工作,那就新建一个线程来处理这个任务,这样就会导致maximumPoolSize太大不能新建线程的error,一般maximumPoolSize设置无限大

  **一个不存储元素的阻塞队列,每个插入操作必须等到另一个线程调用移除操作,否则插入操作一直出于阻塞状态,吞吐量通常高于LinkedBlockingQueue,静态工厂方法Executors.newCachedThreadPool使用了这个队列**

- LinkedBlockingQueue

  - 核心线程数小于核心线程数上限,则新建核心线程处理任务;当核心线程满时会加入队列,而队列无限大,所以会导致maximumPoolSize设置失效

  **一个基于链表结构的阻塞队列,此队列按FIFO(先进先出)排序元素,吞吐类通常高于ArrayBlockingQueue,静态工厂方法Executors.newFixdThreadPool()和Executors.newSingleThreadExecutor()使用了这个队列**

- ArrayBlockingQueue

  - 限定队列的长度,核心线程未满时,就创建核心线程执行任务;核心线程满时就加入队列等待;当队列满时则创建非核心线程进行执行任务;当达到maximumPoolSize抛出异常

  **是一个基于数组结构的有界阻塞队列,此队列按照FIFO(先进先出)原则对元素进行排列**

- DelayQueue

  - 队列内元素必须实现Delayed接口，这就意味着你传进去的任务必须先实现Delayed接口。这个队列接收到任务时，首先先入队，只有达到了指定的延时时间，才会执行任务。

  

- PriorityBlockingQueue

  **一个具有优先级的无限阻塞队列**



#### 7.如何理解有界队列和无界队列

##### 7.1 有界对列

1. 初始化的poolSize<corePoolSize,提交runnable任务,会直接为new一个thread参数立马执行
2. 当提交的任务超过了corePoolSize,会将当前的runnable提交到一个block queue中
3. 有界队列满了之后,如果poolSize<maximumPoolSize,会尝试new一个thread的进行救急处理,立马执行对应的runnable任务
4. 如果`3.`中也无法处理.就会走到第四步执行reject操作

##### 7.2 无界队列

与有界队列相比,除非资源耗尽,否则无界的任务队列不存在入队失败的情况,当有新任务到来时候,系统线程数小于corePoolSize时,则新建执行任务.当达到corePoolSize后就不会继续增加,若后序仍然有信的任务加入,而没有空闲的线程资源,则任务直接进入队列等待;若任务创建和处理的速度差异很大,无界队列会保持快速增长,知道耗尽内存;当线程池的任务缓存队列已满并且线程池中的线程数目达到maximimPoolSize,如果还有任务到来就会采取任务拒绝策略



#### 8 多线程的安全队列一般通过什么实现

java提供的线程安全的Queue可以分为阻塞队列和非阻塞队列,其中阻塞队列的典型例子是BlockingQueue,非阻塞队列的典型例子是**ConcrrentLinkedQueue**



对于BlockingQueue,想要实现阻塞功能,需要调用put(e) take(e)方法.

而ConcurrentLinkedQueue是基于链接结点的,无界的,线程安全的非阻塞队列







