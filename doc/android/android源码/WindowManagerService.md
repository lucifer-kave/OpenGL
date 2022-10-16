Activity在的ATMS和WMS里面都有注册，在ATMS里面的表现为ActivityRecord，在WMS里面表现为WindowState；

WMS里面的AppWindowToken和AMS中的ActivityRecord对应；

#### WMS的功能：

1.添加删除窗口

2.启动窗口

3.窗口动画

4.窗口大小

5.窗口层级：应用的窗口层级低

6.事件派发

####WMS的启动

SystemServer#startOtherServices

WMS needs sensor service ready

WMS和IMS互相持有，WMS持有AMS

####WMS添加Window

com.android.server.wm.WindowManagerService#addWindow

#### Window的删除过程

WindowManagerGlobal#removeVIew()

IWindowSession#remove()