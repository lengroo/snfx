# 每日一遍

水泥封心文案 App：每天翻一篇，扎心但上头。包名 `com.lengroo.snfx`，当前版本 **v0.0.3**。

## 功能

- **两个文案池**：原创长文案 1465 篇 / 抖音热门 365 条，底部按钮切换
- **两种模式**（标题栏右上角）：
  - **默认模式**：抽过的不会再抽到，看完一整轮自动重新开一轮；进度显示「已看 x / 总数」
  - **随机模式**：纯随机，可以抽到之前出现过的（只避开正在显示的那一篇）
- **长按正文复制**，右上角「作者」看作者与版本
- 模式、已看记录、已翻次数都存在 SharedPreferences `dby`，重开 App 继续

## 文案池

| 池子 | 条数 | 来源 |
|---|---|---|
| 原创长文案（默认） | 1465 | `tools/import/` 导入 + `tools/gen_quotes.py` 生成，去重后合并 |
| 抖音热门 | 365 | 全网热门伤感语录合集，`tools/build_hot.py` 从抓取的 markdown 里筛出 |

文案以 `\n@@@\n` 分隔，存在 `app/src/main/assets/quotes.txt` / `hot.txt`。有新文案时：

```bash
python3 tools/merge_own.py   # 把 tools/import/*.txt 合并去重进 quotes.txt
```

## 技术

- 纯 Java + 原生 View，无 Gradle、无 Compose、无第三方库，单个 `MainActivity.java`
- minSdk 21 / targetSdk 33，`build.sh` 直接调 `javac` + `d8` + `aapt` + `zipalign` + `apksigner`
- 深色界面：手写配色 + 圆角卡片 + 按压涟漪，换篇 180ms 淡入

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
app/src/main/assets/quotes.txt   # 原创 1465 篇
app/src/main/assets/hot.txt      # 抖音热门 365 条
app/src/main/res/mipmap-*/       # 五套启动图标（碎心图）
tools/merge_own.py               # 合并去重 tools/import/*.txt → quotes.txt
tools/gen_quotes.py              # 生成 365 篇组合文案 → quotes.txt
tools/extract_hot.py             # 从抓取的 markdown 里抽候选 → hot_pool.json
tools/build_hot.py               # 筛出 365 条热门 → hot.txt + hot_final.json
tools/gen_icon.py                # 程序画一颗碎心图标（xxhdpi）
tools/gen_icon_from_image.py     # 把一张方形图缩放到五套 mipmap 图标
tools/import/                    # 待导入的文案 txt
build.sh                         # 打包脚本（javac + d8 + aapt + zipalign + apksigner）
```

`tools/hot/`（抓取的原始 markdown）与 `tools/raw/`（原始 HTML）没有进仓库，`extract_hot.py` / `build_hot.py` 需要它们时自己放回去。

## 作者

`lengroo错觉与风季一起吃薯条并看着李空壳打CF`
