https://www.kancloud.cn/aslai/interview-guide/1126386

## HashMap

1. JDK7扩容

   - 容量翻倍，阈值=新容量*因子

   - 遍历所有元素可能重新计算hash，头插法（添加元素是尾插法）

2. JDK8

   - 容量翻倍，阈值翻倍

   - 由于数组的容量是以2的幂次方扩容的，那么一个Entity在扩容时，新的位置要么在**原位置**，要么在**原长度+原位置**的位置。

     ![img](/Users/guoqi/Documents/android/typora-user-images/v2-da2df9ad67181daa328bb09515c1e1c8_1440w.png)

     数组长度变为原来的2倍，表现在二进制上就是**多了一个高位参与数组下标确定**。此时，一个元素通过hash转换坐标的方法计算后，恰好出现一个现象：最高位是0则坐标不变，最高位是1则坐标变为“10000+原坐标”，即“原长度+原坐标”。如下图：![img](/Users/guoqi/Documents/android/typora-user-images/v2-ac1017eb1b83ce5505bfc032ffbcc29a_1440w.png)

     因此，在扩容时，不需要重新计算元素的hash了，只需要判断最高位是1还是0就好了。所以JDK8迁移是正序的。

