# 每日一遍

水泥封心文案 App：每天翻一篇，扎心但上头。包名 `com.lengroo.snfx`，当前版本 **v0.04**（versionCode 17）。

## 功能

- **两个文案池**：原创长文案 1465 篇 / 抖音热门 365 条，底部按钮切换
- **两种模式**（标题栏右上角）：
  - **默认模式**：抽过的不会再抽到，看完一整轮自动重新开一轮；进度显示「已看 x / 总数」
  - **随机模式**：纯随机，可以抽到之前出现过的（只避开正在显示的那一篇）
- **中英对照**：每篇中文下面都带英文翻译，1465 篇原创 + 365 条热门全有英文
- **自定义导入**：把你自己写的 txt 导进「当前所在的池子」，一行一篇
- **目录**：卡片底部「目录 · N」打开当前池子全部条目，正在看的那条粉色加粗标出，右上角「定位当前」一键滚到正中；点任意一行直接跳到那篇，并计入已翻次数与已看记录。目录顶部有搜索框，按中文或英文译文实时过滤
- **收藏**：卡片底部「收藏 · N」打开收藏列表，点开跳转，长按移出
- **长按正文菜单**：复制 / 收藏（取消收藏）/ 分享成图
- **分享成图**：1080×1920 深色卡片（中文 + 英文 + 品牌与作者水印），走系统分享面板
- **导入管理**：「导入」弹窗下方「管理已导入」，列出当前池子里自己导入的全部条目，点开跳到那篇、长按删掉那一条、「清空全部」一键清空（二次确认）。删除时同步修正「已看 / 收藏」里的下标，不会错位
- **跟随系统深浅色**：系统切夜间模式时 App 自动重建并换配色（深色底 #0B0B0D / 浅色底 #F4F4F7，卡片 #15151A / #FFFFFF，强调色 #FF5A7A / #D84069），状态栏图标同步反色；分享图固定为深色竖版，不跟随主题
- **左右滑动换篇**：卡片区域左右滑动（水平位移 >56dp 且明显大于竖直位移）即换一篇
- **旋转 / 切主题不断篇**：旋转屏幕或切换深浅色后停在原来那篇（`onSaveInstanceState` 保存当前下标）
- **关于彩蛋**：「关于」标题右侧有一颗「不要点我」小按钮，点开是一段较长的独白，文本在 `assets/nopoint.txt`，想换词直接改这个文件、重打包即可
- **长按正文复制**，右上角「作者」看作者、版本与实时条数
- 模式、已看记录、已翻次数都存在 SharedPreferences `dby`，重开 App 继续

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

- 纯 Java + 原生 View，无 Gradle、无 Compose、无第三方库，`MainActivity.java`（1418 行）+ `ShareCard.java`（分享成图）+ `ShareProvider.java`（FileProvider，供分享时授权图片）
- minSdk 21 / targetSdk 33，`build.sh` 直接调 `javac` + `d8` + `aapt` + `zipalign` + `apksigner`
- 深色界面：手写配色 + 圆角卡片 + 按压涟漪，换篇 180ms 淡入；标题与三个按钮同排，计数行单行省略，关于/导入弹窗统一深色卡片底 + 16dp 圆角 + 粉色主按钮

## 构建

前置：JDK 17、Android SDK（含 `platforms/android-23` 与 build-tools，`aapt` / `zipalign` / `apksigner` / `d8` 在 PATH 或 SDK 里）、`r8.jar`（没有 `d8` 时用）。

签名用的 keystore **不在仓库里**（见 `.gitignore`），自己放一份到 `keystore/release.jks`，密码用环境变量或 `keystore.properties` 给：

```bash
export KS_PASS=<你的 keystore 密码>
# 或者在项目根目录建 keystore.properties：
#   storePassword=<keystore 密码>
#   keyPassword=<key 密码>   （可选，默认同 storePassword）

bash build.sh   # 产物 build/每日一遍.apk
```

工具链路径不对的话，用环境变量覆盖：`JAVA_HOME`、`ANDROID_HOME`（或 `ANDROID_SDK_ROOT`）、`R8_JAR`、`KS`（keystore 路径）。

## 目录

```
app/src/main/java/com/lengroo/snfx/MainActivity.java   # 全部界面与逻辑
app/src/main/java/com/lengroo/snfx/ShareCard.java      # 分享成图：画 1080×1920 深色卡片
app/src/main/java/com/lengroo/snfx/ShareProvider.java  # FileProvider，分享图片授权
app/src/main/assets/quotes.txt   # 原创 1465 篇
app/src/main/assets/quotes_en.txt  # 原创英文 1465 篇（按行号对应）
app/src/main/assets/hot.txt      # 抖音热门 365 条
app/src/main/assets/hot_en.txt   # 热门英文 365 条（按行号对应）
app/src/main/assets/nopoint.txt  # 「不要点我」彩蛋独白
app/src/main/res/mipmap-*/       # 五套启动图标（碎心图）
tools/merge_own.py               # 合并去重 tools/import/*.txt → quotes.txt
tools/gen_quotes.py              # 生成 365 篇组合文案 → quotes.txt
tools/extract_hot.py             # 从抓取的 markdown 里抽候选 → hot_pool.json
tools/build_hot.py               # 筛出 365 条热门 → hot.txt + hot_final.json
tools/translate/                 # 中英翻译对照：zh.json + en_0~4.tsv
tools/gen_icon.py                # 程序画一颗碎心图标（xxhdpi）
tools/gen_icon_from_image.py     # 把一张方形图缩放到五套 mipmap 图标
tools/import/                    # 待导入的文案 txt
build.sh                         # 打包脚本（javac + d8 + aapt + zipalign + apksigner）
```

`tools/hot/`（抓取的原始 markdown）与 `tools/raw/`（原始 HTML）没有进仓库，`extract_hot.py` / `build_hot.py` 需要它们时自己放回去。

## 作者

`lengroo错觉与风季一起吃薯条并看着李空壳打CF`
