
## Android切换语言不重启应用的解决方案

使用了EventBus，注册于反注册

1. **Android程序`内`多语言切换不需要重新启动的解决方案**  
- activity_main不含自定义的支持多语言的控件   
MainActivity中的`切换语言`会改变应用内语言，但没重新加载activity_main，故看不出来  
MainActivity中的`下一个/NEXT`，只在MainActivity的onCreate()被调用即activity_main被重新加载时改变，比如back后再进MainActivity  
只back`不杀死任务栈`，MainActivity会自动缓存之前退出时的configuration？

- MainActivity跳转TestActivity  
activity_test中使用自定义的支持多语言的控件AppTextView、AppButton

- TestActivity跳转LangeChangeActivity  
LangeChangeActivity改变语言后通过EventBus进行post一个自定义的事件  
onStringEvent就会被回调？

- 自定义支持多语言的接口   
根据语言重新加载字符串的方法 在实现类中实现 

2. **Android程序`外`多语言切换不需要重新启动的解决方案**  
在manifest的Activity中配置
`android:configChanges="locale|layoutDirection"`


