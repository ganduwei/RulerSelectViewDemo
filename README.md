# RulerSelectViewDemo
一个像尺子一样的滑动选择控件 RluerSelectView
## 使用方法
```
    布局中
    <com.ganduwei.rulerselectviewdemo.RulerSelectView
            android:id="@+id/rulerSelectView"
            android:layout_width="wrap_content"
            android:layout_height="200dp"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent"/>
            
    代码中
    rulerSelectView.setRulerRange(-20f, 20f)
```
![这里随便写文字](https://github.com/ganduwei/RulerSelectViewDemo/blob/master/ruler14154.gif)
