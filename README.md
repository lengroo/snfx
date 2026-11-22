# 每日一遍

水泥封心文案 App：每天翻一篇，扎心但上头。

包名 `com.lengroo.snfx`，当前版本 **v0.06**（versionCode 59）。纯 Java + 原生 View，无 Gradle、无 Compose、无第三方库。

## 这是什么

一个每天翻一篇文案的小应用。文案分两个池子：原创长文案 1465 篇，抖音热门 365 条，全部中英对照。点「换一篇」翻下一篇，左滑换下一篇，右滑回到上一句，还能朗读、收藏、分享成图，或者把自己写的文案导进来。

没有账号，没有联网，没有广告。模式、已看记录、已翻次数、收藏、排版都存在本机 SharedPreferences `dby`，重开 App 继续。

## 功能

### 翻篇与浏览

- **两个文案池**：原创长文案（默认）/ 抖音热门，底部按钮切换
- **两种模式**（标题栏）：默认模式抽过的不再抽到，看完一整轮自动重开一轮；随机模式纯随机（只避开正在显示的那一篇）
- **滑动换篇**：卡片上左滑换下一篇，右滑回到上一句。历史栈最多 200 条，目录跳转也会进历史。换篇时正文淡入 + 从滑动方向轻滑进来（220 毫秒）
- **目录**：卡片底部「目录 · N」打开当前池子全部条目，正在看的那条粉色加粗标出；标题右侧「定位当前」一键把它滚到列表正中；顶部搜索框按中文或英文译文实时过滤；点任意一行直接跳到那篇，并计入已翻次数与已看记录
- **收藏**：卡片底部「收藏 · N」打开收藏列表，点开跳转，长按移出；收藏分池保存
- **旋转不断篇**：旋转屏幕或切换深浅色后，停在原来那一篇
- **每日一句**：顶栏「今日」按钮，同一天固定同一篇（按年×1000+年内第几天哈希取模）
- **回顾**：顶栏「回顾」按钮，近 7 天每日翻篇数画成小柱状图（今天那根是粉色）+ 收藏数 + 当前池进度 + 连续天数
- **定时提醒**：设置里开，每天到点（默认 22:00）推一条今日一句，点开进 App；重启手机后提醒还在
- **拼音搜索**：目录搜索栏输纯字母（如 `wdn`）按首字母匹配，中英文关键字仍走原文包含；首字母表在 `assets/pinyin.txt`

### 朗读

- 卡片底部「朗读」按钮：用系统 TextToSpeech 念当前这篇，念的时候按钮变成粉色「停止」，点一下即停；念完自动变回「朗读」
- 长按「朗读」选语言：中文（默认，语速 0.95）/ English（用英文译文念，语速 0.92），选择存在 `speak_en`，重开仍生效；没有英文译文时会提示
- 翻页、离开页面、销毁时自动停止朗读并释放引擎
- **连读模式**：设置里开关（默认关），念完 400ms 后自动换下一篇接着念
- Android 11+ 需要包可见性声明，manifest 里已声明 `queries: android.intent.action.TTS_SERVICE`；语音引擎初始化失败时会弹窗引导去系统语音设置，或重试

### 安慰语与设置

- **安慰话**：每翻 10 篇弹一句安慰语（`assets/comfort.txt`，365 条），左按钮退出 App、右按钮继续；设置里可关（`comfort_on`）
- **设置**：顶栏「设置」按钮，里面有安慰开关、连读开关、一键清空浏览记录（二次确认，收藏保留）、导出备份 / 从备份恢复（系统文件选择器，JSON 含收藏/已看/次数/设置/每日统计）

### 排版

- 卡片底部「Aa」打开排版面板（单实例，不会叠加）
- 字号四档：小 / 标准 / 大 / 特大，中文 14/16/18/21sp，英文 11/13/15/17sp
- 行距三档：紧凑 / 标准 / 宽松，中文 6/9/13dp，英文 4/6/9dp
- 选择即时生效，存在 `text_size` / `line_space`

### 外观

- **深浅色**：默认跟随系统，设置里也可以手动选深色或浅色。深色：底 #0B0B0D，卡片 #15151A，强调 #FF5A7A；浅色：底 #F4F4F7，卡片 #FFFFFF，强调 #D84069。状态栏图标随主题反色，涟漪颜色也随主题变
- **启动屏**：Android 12+ 用系统启动屏 API，播一段矢量动画——一颗完整的心放大出现，再从中间裂开（`res/drawable/ic_splash_anim.xml`）；低版本走 `windowBackground` 的 layer-list，立刻就能看到图标，不会先闪空白
- 顶栏：标题、模式按钮、作者、导入在同一条水平线上；进度行与池名行独立成两行，左对齐
- 深色卡片 + 圆角 + 按压涟漪，换篇 180ms 淡入；关于 / 导入弹窗统一深色卡片底 + 16dp 圆角 + 粉色主按钮

### 导入与管理

- 右上角「导入」：把自己写的 txt 导进当前所在的池子，一行一篇
- 「管理已导入」：列出当前池子里自己导入的全部条目，点开跳到那篇，长按删掉那一条，「清空全部」二次确认后清空；删除时同步修正「已看 / 收藏」里的下标，不会错位

### 分享与彩蛋

- **长按正文菜单**：复制 / 收藏（取消收藏）/ 分享成图
- **分享成图**：1080×1920 深色杂志卡片（渐变底 + 右上角淡粉色光晕 + 细描边，顶部小心形与品牌字、右上角日期；正文衬线大字、行距宽松，左上角淡粉色大引号，整块文字垂直居中，太长自动缩小字号；正文与英文之间一条粉色短线，英文斜体灰字；底部细分隔线 + 品牌页脚），另有浅色纸感模板（米白纸底 + 深墨字 + 玫瑰点缀）和红橙黄绿蓝靛紫七色模板（同色系渐变底 + 淡色光晕），分享时九选一，上次的选择会被记住；自建 ShareProvider 通过 `content://` 分享，不申请存储权限；分享图不跟随主题
- **彩蛋**：「关于」标题右侧有一颗「不要点我」小按钮，点开是一段约 1300 字的独白，五段排版，可滚动、可长按选中复制。文本在 `assets/nopoint.txt`，想换词直接改这个文件、重打包即可

## 导入 txt 的格式

一行一篇；空行和以 `#` 开头的行会被忽略。

```
中文文案
中文文案|English translation
# 这是注释，不会导入
你别再试探我了，我不会心软的。|Stop testing me. I will not soften.
我的心是水泥做的，捂不热，也砸不碎。
```

规则：

- 竖线用英文 `|` 或中文 `｜` 都行；竖线后面是英文，可省略
- 每篇至少 4 个字才会被收下
- 和池子里已有文案重复的会被跳过
- 导入后接在池子末尾，原有文案不动；「已看」记录不受影响
- 导入的文案存在 App 私有目录：`files/custom_own.txt`、`custom_hot.txt`（及其 `_en` 版本），卸载 App 才会消失

## 文案池

| 池子 | 条数 | 中文 | 英文 |
|---|---|---|---|
| 原创长文案（默认） | 1465 | `quotes.txt` | `quotes_en.txt` |
| 抖音热门 | 365 | `hot.txt` | `hot_en.txt` |

中英文按行号一一对应，文案以 `\n@@@\n` 分隔，存在 `app/src/main/assets/`。导入的文案没写英文时英文行留空。

有新文案时：

```bash
python3 tools/merge_own.py   # 把 tools/import/*.txt 合并去重进 quotes.txt
```

## 技术

- 纯 Java + 原生 View，`MainActivity.java`（2311 行）+ `ThemeKit.java`（配色与工具）+ `QuoteRepo.java`（文案池、已看/收藏、每日统计、安慰语）+ `Dialogs.java`（设置/回顾/停一下弹窗）+ `Backup.java`（备份导出/恢复）+ `ShareCard.java`（分享成图，九套模板）+ `ShareProvider.java`（FileProvider，供分享时授权图片）
- minSdk 21 / targetSdk 33；manifest 声明 `queries: android.intent.action.TTS_SERVICE`（Android 11+ 查得到语音引擎）
- `build.sh` 直接调 `javac` + `d8` + `aapt` + `zipalign` + `apksigner`，不用 Gradle
- 状态键（SharedPreferences `dby`）：`mode`、`random_mode`、`count_own`、`count_hot`、`seen_own`、`seen_hot`、`fav_own`、`fav_hot`、`speak_en`、`text_size`、`line_space`、`comfort_on`、`continuous`、`daily`

## 构建

前置：JDK 17、Android SDK（含 `platforms/android-34` 与 build-tools，`aapt` / `zipalign` / `apksigner` / `d8` 在 PATH 或 SDK 里）、`r8.jar`（没有 `d8` 时用）。

签名用的 keystore **不在仓库里**（见 `.gitignore`），自己放一份到 `keystore/release.jks`，密码用环境变量或 `keystore.properties` 给：

```bash
export KS_PASS=<你的 keystore 密码>
# 或者在项目根目录建 keystore.properties：
#   storePassword=<keystore 密码>
#   keyPassword=<key 密码>   （可选，默认同 storePassword）

bash build.sh   # 产物 build/每日一遍.apk
```

工具链路径不对的话，用环境变量覆盖：`JAVA_HOME`、`ANDROID_HOME`（或 `ANDROID_SDK_ROOT`）、`R8_JAR`、`KS`（keystore 路径）。

## 目录结构

```
app/src/main/java/com/lengroo/snfx/MainActivity.java   # 主界面与逻辑（2311 行）
app/src/main/java/com/lengroo/snfx/ThemeKit.java       # 配色（深浅两套）与界面工具
app/src/main/java/com/lengroo/snfx/QuoteRepo.java      # 文案池、已看/收藏、每日统计、安慰语
app/src/main/java/com/lengroo/snfx/Dialogs.java        # 设置 / 回顾 / 停一下弹窗
app/src/main/java/com/lengroo/snfx/WeekChartView.java  # 回顾弹窗里的 7 天柱状图
app/src/main/java/com/lengroo/snfx/ReminderReceiver.java  # 每天定时提醒
app/src/main/java/com/lengroo/snfx/Backup.java         # 备份导出 / 恢复（JSON）
app/src/main/java/com/lengroo/snfx/ShareCard.java      # 分享成图：画 1080×1920 卡片，九套模板（深色杂志 / 浅色纸感 / 红橙黄绿蓝靛紫）
app/src/main/java/com/lengroo/snfx/ShareProvider.java  # FileProvider，分享图片授权
app/src/main/assets/quotes.txt   # 原创 1465 篇
app/src/main/assets/quotes_en.txt  # 原创英文 1465 篇（按行号对应）
app/src/main/assets/hot.txt      # 抖音热门 365 条
app/src/main/assets/hot_en.txt   # 热门英文 365 条（按行号对应）
app/src/main/assets/comfort.txt  # 安慰语 365 条
app/src/main/assets/pinyin.txt   # 拼音首字母表（供拼音搜索）
app/src/main/assets/nopoint.txt  # 「不要点我」彩蛋独白
app/src/main/assets/heart_left.png / heart_right.png / icon_heart.png  # 启动动画迭代留下的心形位图（当前版本未引用）
app/src/main/res/mipmap-*/       # 五套启动图标（碎心图）
app/src/main/res/drawable/ic_splash_anim.xml  # 系统启动屏矢量动画（完整心 → 裂开）
app/src/main/res/drawable/ic_splash_vector.xml splash_cover.xml splash_left.xml splash_right.xml splash_scale.xml  # 启动屏矢量与动画片段
app/src/main/res/drawable/splash_window.xml  # windowBackground：纯色底 + 居中图标
app/src/main/res/values/styles.xml  # SplashTheme（windowBackground）
app/src/main/res/values-v31/styles.xml  # SplashTheme：系统启动屏（背景 / 矢量图标 / 450ms）
app/src/main/res/values/colors.xml  # splash_bg 浅色 #F4F4F7
app/src/main/res/values-night/colors.xml  # splash_bg 深色 #0B0B0D
tools/merge_own.py               # 合并去重 tools/import/*.txt → quotes.txt
tools/gen_quotes.py              # 生成 365 篇组合文案 → quotes.txt
tools/extract_hot.py             # 从抓取的 markdown 里抽候选 → hot_pool.json
tools/build_hot.py               # 筛出 365 条热门 → hot.txt + hot_final.json
tools/translate/                 # 中英翻译对照：zh.json + en_0~4.tsv
tools/gen_icon.py                # 程序画一颗碎心图标（xxhdpi）
tools/gen_icon_from_image.py     # 把一张方形图缩放到五套 mipmap 图标
tools/import/                    # 待导入的文案 txt
build.sh                         # 打包脚本（javac + d8 + aapt + zipalign + apksigner）
CHANGELOG.md                     # 从目录功能上线以来的版本更新日志
```

`tools/hot/`（抓取的原始 markdown）与 `tools/raw/`（原始 HTML）没有进仓库，`extract_hot.py` / `build_hot.py` 需要它们时自己放回去。

## 更新日志

版本历史见 [CHANGELOG.md](CHANGELOG.md)，从目录功能上线（v0.04 / code 10）记到现在（v0.06 / code 59）。

## 作者

`lengroo错觉与风季一起吃薯条并看着李空壳打CF`
