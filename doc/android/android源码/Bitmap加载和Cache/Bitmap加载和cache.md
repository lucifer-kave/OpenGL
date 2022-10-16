#### 1. Bitmap和Drawable

可以简单地理解为 Bitmap 储存的是 **像素信息**，Drawable 储存的是 **对 Canvas 的一系列操作**。

而 BitmapDrawable 储存的是「把 Bitmap 渲染到 Canvas 上」这个操作。



#### 2. Bitmap的高效加载

bitmap在android中指的是一个图片,可以png和jpg等其他常见的图片格式,那么如何加载一个图片呢?

BitmapFactory提供四种方法:

- `decodeFile`支持从文件中加载出bitmap对象
- `decodeResource`支持从资源加载出bitmap对象
- `decodeStream`支持从输入流加载出bitmap对象
- `decodeByteArray`支持从字节数组加载出bitmap对象

这几个方法均是在android底层实现,对应`BitmapFactory`几个native方法



**如何高效加载`bitmap`**

核心思想:采用`BitmapFactory.Options`来加载所需尺寸的图片

假设通过imageView来显示图片,很多时候ImageView并没有图片的原始尺寸那么大,这个时候把整个图片加载进来再设置给ImageView,这显然是没有必要的,因为ImageView并没有办法显示原始的图片

**正确做法:**

使用`BitmapFactory.Options`就可以按照一定的采样率来加载缩小后的图片,将缩小后的图片在`ImageView`中显示

**优点:这样可以降低内存占用从而在一定程度上避免OOM,提高了bitmap加载时的性能.**

`BitmapFactory`提供的加载图片的四类方法都支持`BitmapFactory.Options`参数,通过他们可以很方便的对一个图片进行采样缩放;



通过`BitmapFactory.Options`来缩放图片,主要是用到了它的`inSimpleSize`参数,即采样率

- `inSimpleSize=1`,就是原始图片
- `inSimpleSize=2`,宽高均为原图的1/2,而像素数为原图的1/4,其占用内存也是原图的1/4
- `inSimpleSize`必须是大于1的整数图片才会有缩放效果,并且同时作用于宽和高,整体缩放比率为$$1/采样率^2$$;采样率小于1按1处理,原始图片不变
- 最新官方文档之处,`imSimpleSize`取值应该总是2的指数,如`1,2,4,8,16`等,如果传入的`inSimpleSize`不为2的指数,会向下取整,比如3会选择2,但是不是所有android版本都成立

实际中如果imageView是`100*100`,原始图片是`200*300`,如果采样率是3,那么缩放后的图片就小于ImageView所期望的大小,这时候图片就会因为拉伸而变得模糊



通过采样率可以有效的加载图片,那么到底如何获取采样率呢

1. 将`BitmapFactory.Options`的`inJustDecodeBounds`参数设置为true并加载图片
2. 从`BitmapFactory.Options`中去除图片的原始宽高信息,他们对应于`outWidth`和`outHeight`参数
3. 根据采样规则并结合目标View的所需大小计算出采样率`inSimpleSize`
4. 将`BitmapFactory.Options`的`inJustDecodeBounds`参数设置为false,然后重新加载图片

经过上面4个步骤,加载出的图片是最终的目标图片(缩放根据实际场景来定)

参数`inJustDecodeBounds`:

- true:只会解析图片的原始宽高信息,并不会真正的加载图片,所以这个操作是轻量级的.另外需要注意的是,这个时候`BitmapFactory`获取的图片宽/高信息和图片的位置以及程序运行的设备有关,比如同一张图片放置在不用的`drawable`目录下或者程序运行在不同屏幕密度的设备下,都可以导致`BitmapFactory`获取到不同的结果,之所以会出现这个结果,这和android的资源加载机制有关

下面是上诉四个流程的实现:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ImageView mIv = findViewById(R.id.iv);
    // Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.hello);
    Bitmap bitmap = decodeSimpleRes(getResources(), R.mipmap.mao, 100,100);
    mIv.setImageBitmap(bitmap);
}

// 重新设置BitmapFactory的option -- inSampleSize
public Bitmap decodeSimpleRes(Resources res,int resid,int reqWidth,int reqHeight){
    final BitmapFactory.Options options = new BitmapFactory.Options();
    // 设置为true 不直接加载图片
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeResource(res,resid,options);

    // cal inSimpleSize
    options.inSampleSize = calInSimpleSize(options,reqWidth,reqHeight);
    Log.i("hrx", options.inSampleSize + "" );
    options.inJustDecodeBounds = false;

    return BitmapFactory.decodeResource(res,resid,options);
}

// 计算采集率
private int calInSimpleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
    // 获取bitmap大小
    final int height = options.outHeight;
    final int width = options.outWidth;
    int intSimpleSize = 1;
    if (height > reqHeight || width > reqWidth){
        final int halfHeight = height/2;
        final int halfWidth = width/2;
        while ((halfHeight /intSimpleSize) >= reqHeight && (halfWidth/intSimpleSize) >= reqWidth){
            intSimpleSize *= 2;
        }
    }
    return intSimpleSize;
}
```

#### 3. android中的缓存策略

缓存策略在移动端使用场景广泛,尤其在图片加载这个场景下,缓存策略变得更为重要,对于PC用户流量不是那么重要,但是在移动端流量收费,下载图片又比较消耗流量,所以必须提供一种策略来**移动端用户流量消耗的问题**

这就设计到一个**缓存**:就是程序第一次从网络加载图片的时候,就将其缓存到存储设备上;

所以对应三级缓存:(以用户从网络请求图片为例)

1. 先从内存中获取  — 最快
2. 如果内存中没有就从存储设备磁盘上去获取  — 快
3. 最后从网上重新请求  — 慢

三级缓存就提高了程序运行效率,并可以节约流量



**缓存的策略涉及: 缓存的添加,获取和删除这三个操作**

添加获取就是第一次请求网络时候和下一次请求网络时候,比较容易理解

删除主要是无论内存还是磁盘(这里主要指SD卡)有容量限制,一旦容量满了之后,会影响后续添加,这时候需要有一个新旧缓存的增删策略



##### 3.1 LRU

介绍下LRU的策略:

当缓存满时,优先淘汰那些近期最少使用的缓存对象,采用LRU有两种:**内存:LruCache,磁盘DiskLruCache**

###### 3.1.1  LruCache

LruCache是android 3.1提供的一个缓存类,通过support-v4兼容包可以兼容到android早期版本



`LruCache`是一个泛型类,它内部采用一个 `LinkedHashMap`以强引用的方式存储外界的缓存对象,并提供了get和put方法来完成缓存的获取和添加操作,当缓存满时,`LruCache`会移除较早使用的缓存对象,然后添加新的缓存对象:

- 强引用:直接的对象引用,只有引用为空时候会被gc
- 软引用:当一个对象只有软引用存在时,系统内存不足时候会被gc
- 弱引用:当一个对象只有弱引用存在时,此对象随时会被gc掉

`LruCache`是线程安全的,因为`put`和`get`时候会加`synchronized(this)`这样的类锁

```java
@Nullable
public final V get(@NonNull K key) {
    if (key == null) {
        throw new NullPointerException("key == null");
    } else {
        Object mapValue;
        synchronized(this) {
            mapValue = this.map.get(key);
            if (mapValue != null) {
                ++this.hitCount;
                return mapValue;
            }

            ++this.missCount;
        }

        V createdValue = this.create(key);
        if (createdValue == null) {
            return null;
        } else {
            synchronized(this) {
                ++this.createCount;
                mapValue = this.map.put(key, createdValue);
                if (mapValue != null) {
                    this.map.put(key, mapValue);
                } else {
                    this.size += this.safeSizeOf(key, createdValue);
                }
            }

            if (mapValue != null) {
                this.entryRemoved(false, key, createdValue, mapValue);
                return mapValue;
            } else {
                this.trimToSize(this.maxSize);
                return createdValue;
            }
        }
    }
}

@Nullable
public final V put(@NonNull K key, @NonNull V value) {
    if (key != null && value != null) {
        Object previous;
        synchronized(this) {
            ++this.putCount;
            this.size += this.safeSizeOf(key, value);
            previous = this.map.put(key, value);
            if (previous != null) {
                this.size -= this.safeSizeOf(key, previous);
            }
        }

        if (previous != null) {
            this.entryRemoved(false, key, previous, value);
        }

        this.trimToSize(this.maxSize);
        return previous;
    } else {
        throw new NullPointerException("key == null || value == null");
    }
}
```

**LruCache的使用**

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ......
    // 内存转化为KB
    int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    // 总容量大小占整体内存的1/8
    int cacheSize = maxMemory / 8;
  	// 初始化操作
    LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(cacheSize){
        @Override
        protected int sizeOf(@NonNull String key, @NonNull Bitmap value) {
          	// 获取bitmap的大小
            return value.getRowBytes() * value.getHeight();
        }
    };


    // 添加操作
    cache.put("tag_mao",bitmap);
    // 获取操作
    Bitmap tag_mao = cache.get("tag_mao");
    // 删除操作
    cache.remove("tag_mao");
}
```

**DiskLruCache的使用**

**DiskLruCache**不在android源码中,需要自己稍微修改编译错误

1. **DiskLruCache**的创建,没有构造直接`open`

```java
public static DiskLruCache open(File directory, int appVersion, int valueCount, long maxSize)
            throws IOException {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        if (valueCount <= 0) {
            throw new IllegalArgumentException("valueCount <= 0");
        }
        // prefer to pick up where we left off
  			// 实例化`DiskLruCache`
        DiskLruCache cache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
        if (cache.journalFile.exists()) {
            try {
                cache.readJournal();
                cache.processJournal();
                cache.journalWriter = new BufferedWriter(new FileWriter(cache.journalFile, true));
                return cache;
            } catch (IOException journalIsCorrupt) {
                System.logW("DiskLruCache " + directory + " is corrupt: "
                        + journalIsCorrupt.getMessage() + ", removing");
                cache.delete();
            }
        }
        // create a new empty cache
        directory.mkdirs();
        cache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
        cache.rebuildJournal();
        return cache;
    }

static final String JOURNAL_FILE = "journal";
static final String JOURNAL_FILE_TMP = "journal.tmp";

 private DiskLruCache(File directory, int appVersion, int valueCount, long maxSize) {
        this.directory = directory;
        this.appVersion = appVersion;
        this.journalFile = new File(directory, JOURNAL_FILE);
        this.journalFileTmp = new File(directory, JOURNAL_FILE_TMP);
        this.valueCount = valueCount;
        this.maxSize = maxSize;
    }
```

解析一下参数

- `File directory`:
  - 表示磁盘缓存在文件系统中的存储路径,缓存路径可以选择SD卡上的缓存目录,即`/sdcard/Android/package_name/cache`,这里的 package_name 是ap包名,当应用卸载时候这个目录删除,也就是磁盘缓存删除;如果不需要被删除,就选择SD卡下 `/data/`目录下其他目录即可
- `int appVersion`
  - app版本号,一般设置为1即可,如果版本变化改变appVer会清楚缓存目录,不改变appVer则不删除
- `int ValueCount`
  - 单个结点所对应的版本号,一般设置为1即可
- `long maxSize`
  - 表示缓存的大小,`eg:50M`,当超过这个值,这会删除一部分缓存,确保缓存不大于这个值

2. **DiskLruCache**的缓存添加

- **DiskLruCache**的缓存添加操作是通过Editor完成的,Editor表示一个缓存一个缓存对象的编辑对象,这里仍以图片缓存举例,首先获取图片url以及对应的key,然后根据key就可以通过edit()来获取Editor对象,如果这个缓存正在被编辑,那么editor返回一个null,即DiskLruCache不允许同时编译;同一个缓存对象;**之所以将url转换为key是因为url中很可能含有特殊字符,这会影响url的使用;一般情况下,使用url的md5作为key**
- 将图片的url转化为key之后,就可以获取Editor对象了,对于key来说,如果当前不存在其他的Editor对象,那么edit()就会返回一个新的Editor对象,通过他就可以得到一个文件输出流,需要注意的是,由于前面在DiskLruCache的open方法中设置了一个节点只能有一个数据,因此DISK_CACHE_INDEX常量设为0即可;

#### 4.ImagaLoader的实现

DiskLruCache

目标能力:

- 图片同步加载
  - 是指能够以同步的方式向调用者提供锁加载的图片,可以是内存,可以是磁盘,可以是网络拉取
- 图片异步加载
- 图片压缩
  - 较低OOM概率的手段
- 内存缓存
- 磁盘缓存
  - 内存&磁盘缓存可以有效提高程序效率并降低用户流量的消耗
- 网络拉取

##### 4.1 图片压缩功能









