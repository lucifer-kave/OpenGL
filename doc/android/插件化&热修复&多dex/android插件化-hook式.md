#### 1. 前言

占位式插件化有个缺点:就是依赖于宿主的环境,比如context上下文这些;而hook式

插件化不会有这种问题,可以直接使用里面的环境,并不需要重写像`setContentView,findViewById`这类依赖环境的方法

