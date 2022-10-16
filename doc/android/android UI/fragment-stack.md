#### 1. Fragment实现原理和Back Stack

我们知道Activity有任务栈，用户通过startActivity将Activity加入栈，点击返回按钮将Activity出栈。Fragment也有类似的栈，称为回退栈（Back Stack），回退栈是由FragmentManager管理的。默认情况下，Fragment事务是不会加入回退栈的，如果想将Fragment事务加入回退栈，则可以加入`addToBackStack("")`。如果没有加入回退栈，则用户点击返回按钮会直接将Activity出栈；如果加入了回退栈，则用户点击返回按钮会回滚Fragment事务。

```java
if(savedInstanceState == null){
    getSupportFragmentManager()
            .beginTransaction()
            .add(R.id.fr,AFragment.newInstance("你好,LiSa"),"A")
            .addToBackStack("A_f")
            .commit();
}
```

上面这个代码的功能就是将Fragment加入Activity中,内部实现为:创建一个BackStackRecord对象,该对象记录了这个事务的全部操作轨迹(这里只做了一个add操作和加入回退栈),随后将该对象提交到FragmentManager执行队列中,等待执行;

#### 2.getSupportFragmentManager()

```java
// FragmentActivity  --- Activity的base类  作为Activity-fragment的桥梁
public FragmentManager getSupportFragmentManager() {
    return mFragments.getSupportFragmentManager();
}
// FragmentController
public FragmentManager getSupportFragmentManager() {
     return mHost.getFragmentManagerImpl();
}
####  这里返回的就是 FragmentManagerImpl

FragmentManagerImpl  --- 就是ACtivity和fragment关联的容器  也就是FragmentManager的实现类

```



#### 3. .beginTransaction()

创建事务

```java
// 创建BackStackRecord  这个类记录了这个事务的全部操作轨迹
@Override
public FragmentTransaction beginTransaction() {
    return new BackStackRecord(this);
}

/**
 * Entry of an operation on the fragment back stack.
 */
// 主要是执行fragment回退栈的操作
final class BackStackRecord extends FragmentTransaction implements
        FragmentManager.BackStackEntry, FragmentManagerImpl.OpGenerator {
```

- 继承自FragmentTransaction,就是事务用来保存整个事务的全部操作轨迹
- 实现了BackStackEntry,作为回退栈的元素,正是因为该类拥有事务的全部轨迹,因此在popBackStack时能回退整个事务
- 继承Runable,即被放入FragmentManager

#### 4. .add (事务操作)

```java
// BackStackRecord
@Override
public FragmentTransaction add(int containerViewId, Fragment fragment, String tag) {
    doAddOp(containerViewId, fragment, tag, OP_ADD);
    return this;
}

private void doAddOp(int containerViewId, Fragment fragment, String tag, int opcmd) {
        final Class fragmentClass = fragment.getClass();
  
  			// 判断修饰符是不是 public
        final int modifiers = fragmentClass.getModifiers();
        if (fragmentClass.isAnonymousClass() || !Modifier.isPublic(modifiers)
                || (fragmentClass.isMemberClass() && !Modifier.isStatic(modifiers))) {
            throw new IllegalStateException("Fragment " + fragmentClass.getCanonicalName()
                    + " must be a public static class to be  properly recreated from"
                    + " instance state.");
        }
				
  			// fragment设置tag
        fragment.mFragmentManager = mManager;
        if (tag != null) {
            if (fragment.mTag != null && !tag.equals(fragment.mTag)) {
                throw new IllegalStateException("Can't change tag of fragment "
                        + fragment + ": was " + fragment.mTag
                        + " now " + tag);
            }
            fragment.mTag = tag;
        }
				
  			// 设置fragment的res_id
        if (containerViewId != 0) {
            if (containerViewId == View.NO_ID) {
                throw new IllegalArgumentException("Can't add fragment "
                        + fragment + " with tag " + tag + " to container view with no id");
            }
            if (fragment.mFragmentId != 0 && fragment.mFragmentId != containerViewId) {
                throw new IllegalStateException("Can't change container ID of fragment "
                        + fragment + ": was " + fragment.mFragmentId
                        + " now " + containerViewId);
            }
            fragment.mContainerId = fragment.mFragmentId = containerViewId;
        }

        addOp(new Op(opcmd, fragment));
    }
```

这里的核心代码是   `addOp(new Op(opcmd, fragment));`

```
Op(int cmd, Fragment fragment) {
            this.cmd = cmd;
            this.fragment = fragment;
}
    static final int OP_NULL = 0;
    static final int OP_ADD = 1;
    static final int OP_REPLACE = 2;
    static final int OP_REMOVE = 3;
    static final int OP_HIDE = 4;
    static final int OP_SHOW = 5;
    static final int OP_DETACH = 6;
    static final int OP_ATTACH = 7;
    static final int OP_SET_PRIMARY_NAV = 8;
    static final int OP_UNSET_PRIMARY_NAV = 9;
// 上面func中的cmd就是上面定义好的行为
// 这是Op类用来记录操作  cmp记录行为 fragment是当前待操作的fragment  还有就是一些动画

void addOp(Op op) {
    mOps.add(op);
    op.enterAnim = mEnterAnim;
    op.exitAnim = mExitAnim;
    op.popEnterAnim = mPopEnterAnim;
    op.popExitAnim = mPopExitAnim;
}
```

低版本op是一个 链表这里显然不是,仅仅是一个obj,记录了相关属性

这里用list存起来

#### 5.addToBackStack("A_f")

```java
@Override
public FragmentTransaction addToBackStack(String name) {
    if (!mAllowAddToBackStack) {
        throw new IllegalStateException(
                "This FragmentTransaction is not allowed to be added to the back stack.");
    }
    mAddToBackStack = true;
    mName = name;
    return this;
}
```

就是一个布尔值和name的赋值,在commit时候会用到



#### 6.commit()

commit是异步的,即不是立即生效

```java
@Override
public int commit() {
    return commitInternal(false);
}

int commitInternal(boolean allowStateLoss) {
  			// 防止多次commit
        if (mCommitted) throw new IllegalStateException("commit already called");
        if (FragmentManagerImpl.DEBUG) {
            Log.v(TAG, "Commit: " + this);
            LogWriter logw = new LogWriter(TAG);
            PrintWriter pw = new PrintWriter(logw);
            dump("  ", null, pw, null);
            pw.close();
        }
        mCommitted = true;
        if (mAddToBackStack) {
            // 如果添加回退栈 则将事务加入回退栈
            mIndex = mManager.allocBackStackIndex(this);
        } else {
            mIndex = -1;
        }
        mManager.enqueueAction(this, allowStateLoss);
        return mIndex;
    }

 // 将事务加入回退栈
 public int allocBackStackIndex(BackStackRecord bse) {
        synchronized (this) {
            if (mAvailBackStackIndices == null || mAvailBackStackIndices.size() <= 0) {
                if (mBackStackIndices == null) {
                    mBackStackIndices = new ArrayList<BackStackRecord>();
                }
                int index = mBackStackIndices.size();
                if (DEBUG) Log.v(TAG, "Setting back stack index " + index + " to " + bse);
                mBackStackIndices.add(bse);
                return index;

            } else {
                int index = mAvailBackStackIndices.remove(mAvailBackStackIndices.size()-1);
                if (DEBUG) Log.v(TAG, "Adding back stack index " + index + " with " + bse);
                mBackStackIndices.set(index, bse);
                return index;
            }
        }
    }
```

上面第一段代码 `mManager.enqueueAction(this, allowStateLoss);`

将BackStackRecord加入待执行队列中:

```java
public void enqueueAction(OpGenerator action, boolean allowStateLoss) {
    if (!allowStateLoss) {
        checkStateLoss();
    }
    synchronized (this) {
      	// check  Activity 是否销毁
        if (mDestroyed || mHost == null) {
            if (allowStateLoss) {
                // This FragmentManager isn't attached, so drop the entire transaction.
                return;
            }
            throw new IllegalStateException("Activity has been destroyed");
        }
        if (mPendingActions == null) {
            mPendingActions = new ArrayList<>();
        }
      	// 加入待执行队列 
        mPendingActions.add(action);
        scheduleCommit();
    }
}

   private void scheduleCommit() {
        synchronized (this) {
            boolean postponeReady =
                    mPostponedTransactions != null && !mPostponedTransactions.isEmpty();
            boolean pendingReady = mPendingActions != null && mPendingActions.size() == 1;
            if (postponeReady || pendingReady) {
                mHost.getHandler().removeCallbacks(mExecCommit);
                mHost.getHandler().post(mExecCommit);
            }
        }
    }
```

这里使用handler的post方法将线程同步到主线程去更新

```java
Runnable mExecCommit = new Runnable() {
    @Override
    public void run() {
        execPendingActions();
    }
};

/**
   * Only call from main thread!
   */
public boolean execPendingActions() {
        ensureExecReady(true);

        boolean didSomething = false;
        while (generateOpsForPendingActions(mTmpRecords, mTmpIsPop)) {
            mExecutingActions = true;
            try {
                removeRedundantOperationsAndExecute(mTmpRecords, mTmpIsPop);
            } finally {
                cleanupExec();
            }
            didSomething = true;
        }

        doPendingDeferredStart();
        burpActive();

        return didSomething;
    }
```

然后run回调在主线程执行,这里执行fragment的周期





与`addToBackStack()`对应的是`popBackStack()`，有以下几种变种：

- popBackStack()：将回退栈的栈顶弹出，并回退该事务。
- popBackStack(String name, int flag)：name为addToBackStack(String name)的参数，通过name能找到回退栈的特定元素，flag可以为0或者FragmentManager.POP_BACK_STACK_INCLUSIVE，0表示只弹出该元素以上的所有元素，POP_BACK_STACK_INCLUSIVE表示弹出包含该元素及以上的所有元素。这里说的弹出所有元素包含回退这些事务。
- popBackStack()是异步执行的，是丢到主线程的MessageQueue执行，popBackStackImmediate()是同步版本。

