# RefreshLayout
# 一个简单好用的 通用刷新 和加载更多布局。
1. 支持自定义布局 自定义动画帧  自定义滑动距离等。
2. 支持自动滑到底部自动加载更多。支持nestedScrolling惯性滑动.
3. 使用：  implementation 'com.github.fanyaopeng:RefreshLayout:v1.0.1'
4. 自定义style
   ```
    <item name="UniversalRefreshLayoutStyle">@style/APPUniversalRefreshLayout</item>
    <style name="APPUniversalRefreshLayout" parent="UniversalRefreshLayout">
         <item name="max_scroll_distance">50dp</item>
          <item name="head_anim_frame">@mipmap/ic_launcher</item>
          <item name="foot_anim_frame">@mipmap/ic_launcher</item>

    </style>
   ```
   这里head_anim_frame和foot_anim_frame可以是一个AnimationList 代码会自动控制动画的停止与运行
5. 设置自动加载更多
   xml
   ```
      app:auto_load_more="false"
   ```
   或者
   ```
      mRefreshLayout.setAutoLoadMore
   ```
6. 默认效果为
   ![image](https://github.com/fanyaopeng/RefreshLayout/blob/master/images/Screenshot_2019-01-12-19-33-41-09.png)
7. start ! star!

