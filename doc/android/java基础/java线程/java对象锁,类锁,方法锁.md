#### 1.内置锁

java内置锁:每个java对象都可以用作一个实现同步的锁,这些锁称为内置锁.线程进入同步代码块或者方法时候会自动获取该锁,在退出同步代码块或者方法时会释放该锁.获得内置锁的唯一途径就是进入这个锁的保护的同步代码块或者方法



java内置锁是一个互斥锁,这就意味着最多只有一个线程能够获得该锁,当线程A去获得线程B持有的内置锁时候,线程A必须等待或者阻塞,直到线程B释放这个锁,如果B不是放这个锁就永远等待下去

####2. java中对象锁和类锁:

java的对象锁和类锁在锁的概念上基本上和内置锁一致,但是两个锁实际中有很大的区别,对象锁是用于对象实例的方法或者一个对象的实例上,类锁用于一个类的静态方法或者一个类的class对象上.

**我们知道,类的对象实例可以有很多个,但是每个类只有一个class对象,所以不同对象实例的随想锁是互补干扰的.但是每一个类只有一个类锁.但是有一点需要注意的是,其实类锁只是一个概念上的东西,并不是真实存在的,它只是迎来帮助我们理解锁定实例方法和静态方法的区别的**



#### 3. synchronized

在修饰代码块的时候需要一个refernce对象作为锁的对象

在**修饰方法**的时候默认是当前对象作为对象锁

在**修饰类**时候默认是当前类的class对象作为锁的对象



线程同步方法  sychronized,lock,reentrantLock分析





- 对象锁(方法锁):
  - 类中非静态方法上的锁,用this做锁
- 类锁:
  - 类中静态方法上的锁;用xxx.class做锁
- 引用对象作为锁:
  - 用类中成员变量引用做锁

对象锁(方法锁)或者引用对象锁只有对象一直可以实现并发安全

类锁只要类一致,便可实现 线程安全





sychronized使用场景（这些场景，大致能囊括锁使用的大部分情况，有兴趣的可以尝试写写每个案例）

1、一个类的对象锁和另一个类的对象锁是没有关联的，当一个线程获得A类的对象锁时，它同时也可以获得B类的对象锁。

2、一个类中带对象锁的方法和不带锁的方法，可以异步执行，不干扰，不需要等待。

3、一个类中，如果set（）方法加了对象锁，get()方法不加对象锁，容易发生数据脏读。可以用类锁或者set、get都加对象锁，解决。

4、同一个类中，多个锁方法相互调用，线程安全。锁重入，获取锁的对象调用加锁的方法时，会自动获取对象锁，不需要等待。

5、父子类中，锁方法相互调用，线程安全。锁重入，在继承关系中适用。

6、锁方法发生异常时，会自动释放锁。程序继续执行，如果没有响应的处理，会发生业务逻辑的漏洞或者混乱。多见批处理、消息队列等。

7、类中的object成员变量加锁，为任意对象锁。

8、String常量值加锁，容易发生死锁。

9、change锁情况的发生，如果你在一个对象锁方法中，对这个锁对象的引用做了修改，则表示释放了锁对象。如重新new object（），赋值给这个锁对象。但是锁对象的属性或值发生改变，并不发生锁的释放



#### 4,死锁

```java
public class DeadLock {
    public static String obj1 = "obj1";
    public static String obj2 = "obj2";
    public static void main(String[] args){
        Thread a = new Thread(new Lock1());
        Thread b = new Thread(new Lock2());
        a.start();
        b.start();
    }    
}

class Lock1 implements Runnable{
    @Override
    public void run(){
        try{
            System.out.println("Lock1 running");
            while(true){
                synchronized(DeadLock.obj1){
                    System.out.println("Lock1 lock obj1");
                    Thread.sleep(3000);//获取obj1后先等一会儿，让Lock2有足够的时间锁住obj2
                    synchronized(DeadLock.obj2){
                        System.out.println("Lock1 lock obj2");
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
class Lock2 implements Runnable{
    @Override
    public void run(){
        try{
            System.out.println("Lock2 running");
            while(true){
                synchronized(DeadLock.obj2){
                    System.out.println("Lock2 lock obj2");
                    Thread.sleep(3000); //获取obj2后先等一会儿，让Lock1有足够的时间锁住obj1
                    synchronized(DeadLock.obj1){
                        System.out.println("Lock2 lock obj1");
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}

```



Lock1和Lock2是两个task,分别由两个线程`.start`:

- Lock1先获取锁obj1,sleep
- Lock2先获取锁obj2,sleep
- 接着Lock1再去获取obj2,此时obj2已经被Lock2抢到,Lock1等待
- Lock2再去获取锁obj1,此时obj1已经被lock1抢到,Lock2等待
- Lock1和Lock2互相等待,形成死锁

