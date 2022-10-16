#### 1. MVVM含义

- View还是Activity或者fragment,也就是xml,负责页面的展示,或者控制控件的显示和隐藏,以及view的变化,不参与任何逻辑和数据的处理
- ViewModel:主要是负责业务逻辑和数据处理,本身不持有View层的引用,通过LiveData(如果项目中Rxjava可以不引用LiveData)向View层发送数据,通过DataBinding来更改View中的UI层
- Model:实体类javaBean,便是这里的Repository,主要负责从本地数据库或者远程服务器来获取数据,Repository统一了数据的入口,获取数据,将数据发送

