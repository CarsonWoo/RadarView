# RadarView
Android RadarView With Target Result.

#### 效果
![gif](https://img-blog.csdnimg.cn/20200528151838263.gif)

#### How to use

build.gradle (project_name)
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

build.gradle(:app)
``` 
dependencies {
    implementation 'com.github.CarsonWoo:RadarView:1.0.0'
}
```


#### Easy use

```
// when you want to just start the radar
radar_view.start()
// you can also customize the style of the radar 
radar_view.setRadarColor('#00ff00')
// when you want to add image, please call this method first, then call addTarget(size)
radar_view.setBitmapEnabled(true, yourImageRes)
```

#### 具体用法可移步至 https://blog.csdn.net/CarsonWoo/article/details/106405815
