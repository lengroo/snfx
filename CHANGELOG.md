# 每日一遍 · 更新日志

从「目录」功能上线（versionCode 10）到现在（versionCode 27）。
版本名在 code 17→18 时由 0.04 升到 0.05，之后一直沿用 0.05，只递进 versionCode。

## 2026-11-18　v0.04（code 10）— 目录上线
- 卡片底部新增「目录 · N」按钮，点开是当前池子的全部条目列表（ListView，可滑动）
- 点任意一条直接跳到那篇，计入「已翻」与「已看」

## 2026-11-18　v0.04（code 11）— 目录标当前项
- 当前正在看的那一条在目录里用粉色 + 加粗标出
- 目录标题右侧新增「定位当前」按钮，一键滚到当前条目

## 2026-11-18　v0.04（code 12）— 定位居中
- 「定位当前」从“顶到第一行”改成用 setSelectionFromTop 计算偏移，让当前条目落在列表正中
- 版本名按要求回退为 0.04（versionCode 继续递增）

## 2026-11-19　v0.04（code 13）— 关于页彩蛋
- 「关于」标题右侧新增「不要点我」按钮
- 点开显示一段约 600 字的独白（文本放在 assets/nopoint.txt，可随时替换）

## 2026-11-19　v0.04（code 14）— 彩蛋加长
- 独白追加第二段，全文 1330 字，五段排版，可滚动、可长按选中复制

## 2026-11-20　v0.04（code 15）— 收藏 / 搜索 / 分享
- 长按正文弹菜单：复制 / 收藏（取消收藏）/ 分享成图
- 卡片底部新增「收藏 · N」：收藏列表，点开跳转，长按移出；收藏分池保存（fav_own / fav_hot）
- 目录顶部新增搜索框，支持中文与英文译文实时过滤
- 分享成图：1080×1920 深色竖版卡片（中文 + 英文 + 品牌与作者水印），自建 ShareProvider 通过 content:// 分享，不申请存储权限

## 2026-11-20　v0.04（code 16）— 导入管理
- 「导入」弹窗新增「管理已导入」：列出当前池子自己导入的条目
- 点开跳转、长按删除单条、「清空全部」二次确认后清空
- 删除时同步修正「已看 / 收藏」里的下标，不会错位

## 2026-11-20　v0.04（code 17）— 浅色主题 / 跟随系统 / 滑动换篇
- 新增浅色配色并跟随系统深浅色自动切换（背景 #F4F4F7、卡片 #FFFFFF、强调 #D84069）
- 状态栏图标随主题反色；涟漪颜色也随主题变化
- 卡片区域左右滑动换篇（水平位移 >56dp 且明显大于竖直位移，<700ms）
- 旋转屏幕 / 切换深浅色后停在原来那一篇（onSaveInstanceState 保存 currentIndex）

## 2026-11-20　v0.05（code 18）— 上一句 + 朗读
- 历史栈（最多 200 条）：左滑换下一句，右滑回到上一句；目录跳转也进历史
- 长按菜单新增朗读（系统 TextToSpeech，中文，语速 0.95）
- 翻页 / 离开页面 / 销毁自动停止朗读并释放引擎
- 版本名升为 0.05

## 2026-11-20　v0.05（code 19）— 朗读修复
- 修复「语音引擎初始化失败」：Android 11+ 包可见性导致查不到 TTS_SERVICE，manifest 补 <queries>
- 语言先试 SIMPLIFIED_CHINESE 再退回 CHINESE
- 失败改为弹窗引导：打开语音设置 / 重试 / 关闭

## 2026-11-20　v0.05（code 20）— 朗读按钮下移
- 卡片底部新增「朗读」按钮，朗读中变粉色「停止」，点一下即停

## 2026-11-20　v0.05（code 21）— 提示文案修正
- 朗读提示由「长按可停止」改为「点底部『停止』可结束」

## 2026-11-20　v0.05（code 22）— 菜单精简
- 长按菜单去掉朗读项，朗读只保留底部按钮这一个入口

## 2026-11-21　v0.05（code 23）— 朗读语言
- 长按「朗读」弹出语言选择：中文（默认）/ English
- 英文朗读使用译文、语速 0.92；选择存 speak_en，重开仍生效
- 没有英文译文时提示「这篇没有英文译文」

## 2026-11-21　v0.05（code 24）— 朗读结束复位
- 用 UtteranceProgressListener 监听 onDone / onError，念完自动把按钮从「停止」变回「朗读」

## 2026-11-21　v0.05（code 25）— 字号 / 行距
- 卡片底部新增「Aa」打开排版面板
- 字号：小 / 标准 / 大 / 特大 → 中文 14/16/18/21sp，英文 11/13/15/17sp
- 行距：紧凑 / 标准 / 宽松 → 中文 6/9/13dp，英文 4/6/9dp
- 即时生效并存 text_size / line_space

## 2026-11-21　v0.05（code 26）— 顶栏对齐
- 标题、模式按钮、作者、导入改为同一行、同一水平中线（headerRow gravity=CENTER_VERTICAL）
- 进度行与池名行独立成两行，左对齐 20dp

## 2026-11-21　v0.05（code 27）— 排版弹窗单实例
- 修复改字号时弹窗层层叠加、关闭只关最上面一张的问题
- 现在先 dismiss 旧面板再重开，「关闭」一次全关

## 2026-11-22　v0.05（code 28）— 启动动画
- 新增 SplashView 遮罩：一颗完整的心从中间裂开（锯齿缺口由窄变宽），最后停在碎裂图标的状态
- 时间线：裂开 900ms（Decelerate）→ 停留 320ms → 淡出 320ms → 移除遮罩
- 心形用参数方程 x=16sin³t / y=13cost−5cos2t−2cos3t−cos4t 采样 240 点绘制，颜色随深浅主题

## 2026-11-22　v0.05（code 29）— 启动动画改用图标素材
- 不再手绘心形，直接从图标原图（1600×1600）抠掉米白背景、裁切、缩到 512px，存 assets/icon_heart.png
- 动画改成「两半合拢 → 向外滑开」：同一张图左右各画一次、按中线裁剪，位移从 5.5% 收到 0，最后停在图标原样
- 素材 106 KB，整包 346 KB

## 2026-11-22　v0.05（code 30）— 抄 Nagram 的启动屏写法
- 逆向 Nagram v13.2.2：它用 Android 12 系统启动屏 API，在 res/values-v31/styles.xml 里声明
  windowSplashScreenBackground / windowSplashScreenAnimatedIcon / windowSplashScreenAnimationDuration=450
- 动画本体是 animated-vector + objectAnimator(propertyName=pathData, valueType=pathType)，把飞机图标从点「长」出来
- 我们照同样结构实现：新增 SplashTheme（values + values-v31），启动屏图标用 @mipmap/ic_launcher、时长 450ms、背景 #0B0B0D
- 构建脚本改用 API34 的 android.jar（API23 的 jar 里没有这些属性）
- 自绘的「两半滑开」遮罩仍保留，所以 12+ 也能看到裂开那一幕

## 2026-11-22　v0.05（code 31）— 启动屏底色跟随主题
- splash_bg 拆成 values / values-night 两套：#F4F4F7 / #0B0B0D，系统启动屏不再先闪一下深色
- 构建换用 Android 14（API34）android.jar，compileSdk 随之变成 34

## 2026-11-22　v0.05（code 33）— 启动图标改成矢量（抄 Nagram 的 animated-vector）
- 用 potrace 把图标轮廓描成矢量路径（512×512，312 个曲线点），不再手绘
- 新增 res/drawable/ic_splash_vector.xml（外层 0.78 缩放防系统裁切）、splash_scale.xml（scaleX/scaleY 0.35→1，450ms，fast_out_slow_in）、ic_splash_anim.xml（animated-vector）
- windowSplashScreenAnimatedIcon 指向 @drawable/ic_splash_anim，和 Nagram 一模一样的注册方式
- Android 12+ 交给系统播这段矢量动画；低版本仍用自绘的缩放缓入遮罩（不再有「两半滑开」）

## 2026-11-22　v0.05（code 34→35）— 修启动矢量
- code 34：potrace 掩码反了，描出来的是背景（方块挖心洞），已把掩码反过来重描
- code 35：potrace 的坐标包在 translate(0,512) scale(0.1,-0.1) 里，之前漏了这个变换，导致心被放大 10 倍并上下翻转；现在用 Android group 的 translateY=512 / scaleX=0.1 / scaleY=-0.1 一比一还原
- 校验：把 SVG 渲染成 128px 与图标掩码比对，IoU=0.972，四角为空（确认是心不是方块）

## 2026-11-22　v0.05（code 36）— 启动屏加回「完整→裂开」
- 用锯齿线（振幅 5px、周期 64px）把心形掩码切成左右两半，各自用 potrace 描矢量
- 合拢态 IoU 0.983（与图标几乎重合，看起来是完整的心）；裂开态 ±7 单位位移，IoU 0.868（中间露出锯齿裂缝）
- animated-vector 三个 target：scale_g（0.55→1，450ms）、left_g / right_g（translateX 0→∓7，300ms，延迟 140ms 起跑）

## 2026-11-22　v0.05（code 37）— 启动裂开改用位图两半
- 矢量 AVD 在系统启动屏上比例失真（渲染成竖长条），放弃该路线；系统启动屏图标换回 @mipmap/ic_launcher
- 改为在 App 内用位图两半：沿锯齿线切出 heart_left.png / heart_right.png，合拢与原图 IoU=1.0、裂开 IoU=0.902
- SplashView：0.62→1 放大 + 透明度渐入，同时两半各向外移 size*1.4% 露出裂缝；460ms → 停 260ms → 淡出

## 2026-11-22　v0.05（code 38）— 只保留系统启动屏动画
- 删除 App 内的 SplashView 遮罩与 playSplash()，系统启动屏结束后不再叠一层动画
- windowSplashScreenAnimatedIcon 改回 @drawable/ic_splash_anim（两半矢量 + 3 段 AVD）
- 矢量外层加 0.72 留边，避免被 Android 12 的圆形遮罩裁掉心的左右两角
- 离线校验：起始/合拢/裂开三态归一化 IoU 0.88 / 0.93 / 0.84，宽高比 1.00–1.04（不再是竖长条）

## 2026-11-22　v0.05（code 39）— 起始是完整的心
- 矢量里加第三条路径「cover」：整颗心的实心剪影，画在左右两半之上，把裂缝盖住
- 动画：0–170ms 完整心（含 0.55→1 放大）；170ms 起 cover 的 fillAlpha 1→0（260ms），同时左右两半 translateX 0→∓10（280ms）
- 因为三条路径同色同位置，交叉淡出看不出接缝，只看到裂缝慢慢出现

## 2026-11-22　v0.05（code 42）— 系统启动屏播得完了
- Android 12+ 的 onCreate 用 Handler 延后 520ms 再 setContentView（startUi），否则第一帧一画系统启动屏立刻收掉，450ms 的矢量动画被截断
- 12 以下逻辑不变（直接 startUi）

## 2026-11-22　v0.05（code 43）— 修复小米出现空白启动画面
- 去掉 code 42 的 520ms 延迟：部分 ROM 忽略 windowSplashScreen* 时，延迟只会让人盯着空白窗口
- windowBackground 改为 layer-list（splash_window.xml）：纯色底 + 居中的图标 bitmap 放大 260%，任何 ROM 都能立刻看到图标
- 支持系统启动屏的机型仍走 @drawable/ic_splash_anim 的矢量动画
