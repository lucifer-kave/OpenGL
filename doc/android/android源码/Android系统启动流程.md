**Android系统启动流程：**

**1、启动电源以及系统启动：**

当电源键按下时引导芯片代码从预定义的地方（固化在ROM）开始执行。加载引导程序BootLoader到RAM中，然后执行。

**2、引导程序BootLoader：**

引导程序BootLoader是在Android操作系统开始运行前的一个小程序，它的主要作用是把系统OS拉起来并运行。

**3、Linux内核启动：**

当内核启动时，设置缓存、被保护存储器、计划列表、加载驱动。当内核完成系统设置时，它首先在系统文件中寻找init.rc文件，并启动init进程。

**4、init进程启动：**

初始化和启动属性服务，并且启动Zygote进程。

**5、Zygote进程启动：**

创建java虚拟机并为java虚拟机注册JNI方法，创建服务器端Socket，启动SystemServer进程。

**6、SystemServer进程启动：**

启动Binder线程池和SystemServiceManager，并且启动各种系统服务。