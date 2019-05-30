# RulerSelectViewDemo
一个像尺子一样的滑动选择控件 RluerSelectView ,支持在ViewPager等滑动控件中使用。
## 使用方法
```java
布局中
<TextView
        android:id="@+id/textView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="28sp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintVertical_bias="0.2"/>
<com.ganduwei.rulerselectviewdemo.RulerSelectView
        android:id="@+id/rulerSelectView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>
            
代码中
rulerSelectView.setRulerRange(-20f, 20f)
rulerSelectView.setRulerSelectChangeListener { rulerSelectView, selectedPosition, selectedValue ->
    textView.text = selectedValue.toString()
}
```
## 效果图
![这里随便写文字](https://github.com/ganduwei/RulerSelectViewDemo/blob/master/ruler14154.gif)

## 实现原理解析
https://blog.csdn.net/ganduwei/article/details/89526222

