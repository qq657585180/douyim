# 抖X 设置项字段清单（已解密）

> 来源：抖X_261021231933.apk settings/n 加密串全量还原（AES-256-GCM 已破解）
> 用途：移植到 douyim（抖仙人）时的字段对照表

## 分组统计
- **comment**（评论）：46 项
- **appearance**（外观）：64 项
- **playback**（播放）：33 项
- **other**（其他）：176 项
- **download**（下载）：11 项
- **message**（消息）：15 项
- **profile**（主页）：5 项
- **live**（直播）：9 项

## 设置项详情

### playback（播放）

| Key | 标题 | 描述/选项 |
|---|---|---|
| playback_strategy | 播放与交互策略 | 完成暂停<br>合集<br>下一集<br>双击点赞<br>返回刷新<br>关注确认<br>画幅<br>溢出 |
| page.quality | 播放画质 |  |
| progress.20ee30e2ae | 常驻进度条 |  |
| playback.time_independent | 时间独立常显 | 常显时间 |
| playback.time_position | 时间位置 | 进度时间 |
| playback.time_size | 时间字号（sp） | 进度时间 |
| playback.time_color | 时间颜色 | 进度时间 |
| playback.time_opacity | 时间透明度（%） | 时间不透明度（%） |
| playback.time_margin | 时间水平边距（dp） | 进度时间 |
| playback.time_bottom | 时间底部边距（dp） | 进度时间 |
| playback.progress_time | 显示播放时间 | 进度时间<br>播放时间<br>时间 |
| extras.d2a3ea57f1 | 自动跳过章节广告 |  |
| extras.739e412c3a | 离线缓存数量 |  |
| gestures.37b88fc1fb | 清爽模式设置 |  |
| gestures.a22fbb4c39 | 长按 |  |
| gestures.26d7c24f77 | 双击 |  |
| gestures.be54e501cb | 临时倍速 |  |
| extras.preload | 预加载策略 | Feed预加载<br>禁用预加载 |
| extras.preload_count | 预加载数量 |  |
| extras.preload_quality | 预加载清晰度 |  |
| extras.recall | 保护当前缓存作品 | 缓存失效保护 |
| extras.keep_follow | 保留关注流缓存作品 |  |
| quality.proxy | 原画本地代理 | 回环代理 |
| quality.whole_cache | 小视频整文件缓存 |  |
| quality.dd11313c92 | 播放器诊断信息 |  |
| quality.4360cc0065 | 原画仅 Wi-Fi |  |
| danmaku.83d6b31d9a | 启用弹幕增强 |  |
| danmaku.1d6c3a1841 | 屏蔽关键词 |  |
| danmaku.01c0117c23 | 重复弹幕去重 |  |
| danmaku.a1ce67ce92 | 解除抖音屏蔽词 30 条限制 |  |
| danmaku.ea1dc8805e | 打开评论区时保留弹幕 |  |
| danmaku.9daa16e0ab | 弹幕颜色 |  |
| progress.hidden | 隐藏视频进度条 |  |

### comment（评论）

| Key | 标题 | 描述/选项 |
|---|---|---|
| playback | 视频播放 | 倍速、进度条、连播、画质与手势<br>评论区<br>外观、时间、交互、过滤、图片与语音<br>消息隐私<br>聊天工具、已读与输入状态、撤回与续火<br>净化过滤<br>内容过滤、弹幕、清爽模式与广告跳过<br>外观界面<br>界面风格、首页底栏、控件外观与液态玻璃<br>下载收藏<br>下载、分享栏、长按菜单与插眼<br>直播<br>画质、人数、清爽与直播语音<br>系统数据<br>speed progress autoplay quality extras gestures longpress_playback playback_strategy |
| comment.show_hidden_emoji | 显示宿主隐藏表情 | 隐藏小表情 |
| comments.96c0e404c6 | 评论时间格式与模板 |  |
| comments.b213d022be | 复制去除艾特前缀 |  |
| comments.7cd923e2c7 | 评论交互 |  |
| comments.753d08c43d | 我的评论与互动 |  |
| comments.2bb0831baa | 评论区外观 |  |
| comments.84f54a63d8 | 评论分页隐藏 |  |
| comments.81672f97bd | 评论语音替换 |  |
| comments.image_original | 原生评论图片无水印 | 评论图片保存原图 |
| comments.audio_download | 评论语音快捷下载 | 双击语音<br>绿色下载按钮<br>WAV |
| comments.950e8a1e12 | 语音入口长按选择文件 |  |
| comments.cc7c36fa92 | 长按菜单项目 |  |
| comments.5f95f3003e | 评论过滤 |  |
| comments.ae21a37d1c | 长按扩展菜单 |  |
| comment.time | 显示完整时间 | 评论时间<br>秒<br>时间 |
| comment.region | 长按查看 IP 属地 | 独立入口查看并复制抖音提供的完整属地 |
| comments.70151eae18 | 显示准确点赞数 |  |
| comment_interaction.73cc3f4abf | 始终显示位置入口 |  |
| comment_interaction.47dd5a3881 | 插入位置前编辑文字 |  |
| comment_interaction.ea615e3e1d | 发布蓝字评论 |  |
| comment_interaction.6ef5a30dbc | 打开评论暂停视频 |  |
| comment_interaction.f89dd25379 | 点赞操作前确认 |  |
| comment_interaction.9d018d123f | 点踩操作前确认 |  |
| comment_interaction.ea45e2f234 | 隐藏顶部广告与相关搜索 |  |
| comment_interaction.1ffb4cb3f8 | 隐藏点赞按钮 |  |
| comment_interaction.008507accf | 隐藏点踩按钮 |  |
| comment_interaction.51e3399312 | 隐藏回复按钮 |  |
| comment_interaction.ae313f9a5c | 隐藏底部输入栏 |  |
| comment_interaction.fb9a1f8613 | 禁用点击评论回复 |  |
| comment_interaction.6346a6d3d4 | 打开评论不缩放视频 |  |
| comment_interaction.disable_shift | 打开评论作品不平移 | 阻止评论区打开时作品向上位移 |
| comment_interaction.c0ce370b93 | 空评论不自动弹出输入框 |  |
| comment_appearance.503606eb6d | 评论文字颜色 |  |
| comment_appearance.ebbd320f5f | 自定义评论背景 |  |
| comment_appearance.2d7b8ec9a3 | 动态模糊背景 |  |
| comment_appearance.882fe2e842 | 背景颜色 |  |
| comment_appearance.37dc6e6be5 | 图片缩放 |  |
| comment_appearance.7a4df1f81a | 使用旧版评论区 |  |
| comment_filter.5e6db5d12c | 启用评论过滤 |  |
| comment_filter.2ecfeaf382 | 评论关键词 |  |
| comment_filter.c28bbe3819 | 屏蔽评论用户 |  |
| comment_filter.6f055918e2 | 评论用户白名单 |  |
| comment_filter.cd2238182a | 过滤广告评论 |  |
| comment_appearance.60c73f44b9 | 面板高度 |  |
| comment_appearance.9eaa61edf0 | 背景透明度 | 背景不透明度 |

### message（消息）

| Key | 标题 | 描述/选项 |
|---|---|---|
| message.friends | 隐藏消息页顶部朋友 | 消息朋友 |
| messages.3e3feb2a50 | 消息长按工具 |  |
| messages.30ce80fcb1 | 右滑引用回复 |  |
| messages.ad76394fb6 | 聊天语音工具 |  |
| messages.0d6bc93404 | 收藏表情管理 |  |
| messages.5583c877cb | 消息一键已读 |  |
| messages.6e933e99e2 | 导出聊天记录 |  |
| messages.ff3e0ab8e6 | 自动续火 |  |
| privacy.9204803a27 | 阻止已读上报 |  |
| privacy.b9e6907634 | 阻止正在输入上报 |  |
| privacy.ac66d7ae6c | 阻止聊天截图通知 |  |
| privacy.c7ea651bf4 | 显示本地撤回原文 |  |
| privacy.dab943c438 | 阻止仅看一次销毁 |  |
| privacy.49e47e3d4a | 开启仅看一次入口 |  |
| privacy.97e76da8b0 | 本地访客开启标识 |  |

### appearance（外观）

| Key | 标题 | 描述/选项 |
|---|---|---|
| report_issue | 提交问题报告 | 手动提交错误<br>反馈问题<br>材质<br>磨砂<br>液态玻璃<br>清透玻璃 |
| glass.bottom.reference | 参考分组底栏 | 闪电<br>纸飞机<br>快捷操作 |
| glass.top | 顶部导航液态玻璃 | 频道<br>折叠<br>展开<br>玻璃 |
| glass.right | 视频按钮液态玻璃 | 点赞<br>评论<br>收藏<br>分享<br>玻璃 |
| glass.bottom.detail.0 | 发布入口独立成圆 | 清透玻璃<br>胶囊 |
| glass.bottom.detail.1 | 融合玻璃进度条 | 清透玻璃<br>胶囊 |
| glass.bottom.detail.2 | 玻璃进度条点击跳转 | 清透玻璃<br>胶囊 |
| glass.bottom.detail.3 | 底栏背景模糊 | 清透玻璃<br>胶囊 |
| glass.bottom.detail.4 | 底栏只显示图标 | 清透玻璃<br>胶囊 |
| glass.bottom.detail.5 | 姿态感知高光 | 清透玻璃<br>胶囊 |
| glass.bottom.detail.6 | 底栏缩放 | 清透玻璃<br>胶囊 |
| glass.bottom.detail.7 | 底栏位置偏移 | 清透玻璃<br>胶囊 |
| glass.bottom.detail.8 | 底栏图标大小 | 清透玻璃<br>胶囊 |
| glass.bottom.detail.9 | 边缘折射强度 | 清透玻璃<br>胶囊 |
| glass.bottom.detail.10 | 边缘高光强度 | 清透玻璃<br>胶囊 |
| glass.bottom.detail.11 | 按压色散强度 | 清透玻璃<br>胶囊 |
| glass.bottom.detail.12 | 按压内阴影强度 | 清透玻璃<br>胶囊 |
| glass.bottom.thickness | 玻璃进度条厚度 | 进度条粗细<br>厚度<br>清透玻璃 |
| glass.bottom.detail.13 | 背景采样间隔 | 清透玻璃<br>胶囊 |
| glass.module.blur | 模块弹窗液态玻璃 · 模糊半径 | 模块弹窗液态玻璃模糊半径 |
| glass.module.opacity | 模块弹窗液态玻璃 · 透明度 | 模块弹窗液态玻璃 · 不透明度<br>模块弹窗液态玻璃不透明度<br>模块弹窗液态玻璃透明度 |
| glass.bottom.blur | 首页底栏液态玻璃 · 模糊半径 | 首页底栏液态玻璃模糊半径 |
| glass.bottom.opacity | 首页底栏液态玻璃 · 透明度 | 首页底栏液态玻璃 · 不透明度<br>首页底栏液态玻璃不透明度<br>首页底栏液态玻璃透明度 |
| glass.comment.blur | 评论面板液态玻璃 · 模糊半径 | 评论面板液态玻璃模糊半径 |
| glass.comment.opacity | 评论面板液态玻璃 · 透明度 | 评论面板液态玻璃 · 不透明度<br>评论面板液态玻璃不透明度<br>评论面板液态玻璃透明度 |
| page.glass | 清透玻璃 | 玻璃<br>模糊 |
| glass.avatar | 底栏显示账号头像 | 头像 |
| glass.double | 顶栏双列浏览入口 | 双列 |
| glass.fps | 顶栏实时帧率 | Hz |
| glass.module | 模块弹窗液态玻璃 | 玻璃<br>模糊 |
| glass.bottom | 首页底栏液态玻璃 | 玻璃<br>模糊 |
| glass.comment | 评论面板液态玻璃 | 玻璃<br>模糊 |
| home.top_center | 顶部标签居中 |  |
| home.bottom_hidden | 隐藏整个首页底栏 |  |
| home.bottom_keep_space | 隐藏底栏时保留占位 |  |
| home.c25b735dcd | 启用首页整理 |  |
| home.cd1bda47ff | 底栏图标与文字 |  |
| home.7c24a0e2fc | 底栏沉浸 |  |
| home.065750266b | 双列与 AI 搜索入口 |  |
| home.c4c8063945 | 顶部标签 |  |
| home.3e1d8ed19a | 底部菜单 |  |
| home.6c47740864 | 沉浸显示 |  |
| home.9902412e18 | 透明系统导航栏 |  |
| home.2e0ddfde95 | 播放时隐藏系统状态栏 |  |
| home.7616bc18d9 | 隐藏系统导航栏 |  |
| home.71d978e90b | 显示双列入口 | 双列与 AI 搜索入口 |
| home.c8c0dc2845 | 显示 AI 搜索入口 | 双列与 AI 搜索入口 |
| home.d9b79bb5a2 | 隐藏侧栏入口 |  |
| home.d23c896621 | 隐藏首页搜索 |  |
| home.3fe35ca5c8 | 隐藏消息角标 |  |
| home.6f099dad58 | 首页按钮刷新前确认 |  |
| home.sidebar_tools | 左滑侧栏：模块设置与插眼 | 左滑<br>侧栏<br>模块入口<br>模块设置 |
| home.ba08b02cee | 隐藏首页底栏皮肤 |  |
| controls.e5681e7ff3 | 启用控件隐藏 |  |
| controls.24a33047b2 | 播放与暂停样式 |  |
| controls.9aa386bb74 | 右侧显示真实数量 |  |
| controls.a98f8afb28 | 音乐封面随右侧栏移动 |  |
| controls.fae145829a | 选择隐藏控件 |  |
| controls.ad_card | 隐藏应用广告卡片 | 下载广告<br>打开广告<br>应用推广<br>红果广告 |
| controls.report_warn | 隐藏作者声明／风险提示 | 作者声明<br>AI声明<br>内容由AI生成 |
| controls.d4f77759dc | 清爽模式 |  |
| controls.6f9739b634 | 控件外观 |  |
| controls.1cdcf3fb52 | 播放背景 |  |
| controls.1f40c9cbb8 | 可视化隐藏控件 |  |

### download（下载）

| Key | 标题 | 描述/选项 |
|---|---|---|
| page.download | 下载格式与目录 |  |
| paths.0b7e287692 | 保存位置 |  |
| paths.1b6cfe60d5 | 改回系统相册／音乐 |  |
| paths.d094b9e3ec | 路径模板（不含扩展名） |  |
| paths.7530fa1a19 | 日期格式 |  |
| download_interaction.b3668c01d7 | 抖音通知权限 |  |
| download_interaction.60b3a39899 | 保存前确认文件名 |  |
| download_interaction.4512fa866c | 下载窗口默认最小化 |  |
| download_interaction.2c25d425da | 显示下载通知 |  |
| download.e4e86807ea | 视频格式 |  |
| download.3b44f4c721 | 保存子目录 |  |

### live（直播）

| Key | 标题 | 描述/选项 |
|---|---|---|
| page.live | 直播增强 |  |
| live.voice_replace | 直播语音替换 | 直播 MP3<br>直播语音评论 |
| live.0d1ad71357 | 直播具体人数 |  |
| live.41fb1b555b | 直播默认画质 |  |
| live.964ffe7b61 | 直播信息入口 |  |
| live.0e64c35bc2 | 当前直播信息 |  |
| live.7420f518ca | 直播清爽 |  |
| live.737df7edc2 | 移动网络后台续播 |  |
| comment_media | 自动续火 | 续火任务管理<br>直播信息入口<br>长按人数查看直播信息 |

### profile（主页）

| Key | 标题 | 描述/选项 |
|---|---|---|
| page.profile | 个人主页 | 主页精确数量<br>作品网格精确点赞 |
| profile.exact_counts | 主页精确数量 |  |
| profile.exact_grid_likes | 作品网格精确点赞 |  |
| profile.7279f9e6d6 | 默认打开作品页 |  |
| glass_video | 本地访客开启标识 | 原生评论图片无水印<br>评论语音快捷下载<br>评论语音替换<br>语音入口长按选择文件<br>显示宿主隐藏表情 |

### other（其他）

| Key | 标题 | 描述/选项 |
|---|---|---|
| module_theme | 界面风格 | 设置界面主题<br>自动<br>亮色<br>暗黑<br>主题模式<br>轻羽<br>星章<br>天空岛<br>头图<br>背景图<br>装饰动效<br>原神 |
| iconpacks | 主题图标包 | ZIP<br>PNG<br>GIF<br>整包切换<br>主题 |
| quality | 默认播放画质 |  |
| quality | 视频解码方式 |  |
| styles_video | 播放与暂停样式 |  |
| styles_home | 底栏图标与文字 |  |
| style__top | 顶部导航样式 |  |
| style__top | 顶部导航 · 透明度 | 顶部导航 · 不透明度<br>透明度 |
| style__top | 顶部导航 · 暂停透明度 | 顶部导航 · 暂停不透明度<br>暂停透明度 |
| style__top | 顶部导航 · 暂停时使用独立透明度 | 顶部导航 · 暂停时使用独立不透明度<br>暂停时使用独立不透明度<br>暂停时使用独立透明度 |
| style__bottom | 底部导航 · 透明度 | 底部导航 · 不透明度 |
| style__bottom | 底部导航 · 暂停透明度 | 底部导航 · 暂停不透明度 |
| style__bottom | 底部导航 · 暂停时使用独立透明度 | 底部导航 · 暂停时使用独立不透明度<br>暂停时使用独立不透明度 |
| style__overlay | 视频覆盖控件样式 |  |
| style__overlay | 视频覆盖控件 · 透明度 | 视频覆盖控件 · 不透明度 |
| style__overlay | 视频覆盖控件 · 暂停透明度 | 视频覆盖控件 · 暂停不透明度 |
| style__overlay | 视频覆盖控件 · 暂停时使用独立透明度 | 视频覆盖控件 · 暂停时使用独立不透明度<br>暂停时使用独立不透明度 |
| style__right | 视频右侧栏样式 |  |
| style__right | 视频右侧栏 · 透明度 | 视频右侧栏 · 不透明度 |
| style__right | 视频右侧栏 · 暂停透明度 | 视频右侧栏 · 暂停不透明度 |
| style__right | 视频右侧栏 · 暂停时使用独立透明度 | 视频右侧栏 · 暂停时使用独立不透明度<br>暂停时使用独立不透明度 |
| style__right | 视频右侧栏 · 左右移动（dp） | 左右移动（dp） |
| style__right | 视频右侧栏 · 上下移动（dp） | 上下移动（dp） |
| style__right | 视频右侧栏 · 缩放（0.1–3 倍） | 缩放（0.1–3 倍） |
| styles_video | 替换文字 | 先选择控件再调整 |
| styles_video | 选择文字位置 | 先选择控件再调整 |
| styles_video | 选择图标位置 | 先选择控件再调整 |
| styles_video | 分别设置选中与未选中图标 | 先选择控件再调整 |
| styles_video | 选择图标图片 | 先选择控件再调整 |
| styles_video | 清除图标图片 | 先选择控件再调整 |
| styles_video | 选择选中状态图标 | 先选择控件再调整 |
| styles_video | 清除选中状态图标 | 先选择控件再调整 |
| styles_home | 先选择控件再调整 |  |
| styles_home | 选择文字位置 | 先选择控件再调整 |
| styles_home | 选择图标图片 | 先选择控件再调整 |
| styles_home | 先选择控件再调整 |  |
| streak | 选择会话 |  |
| streak | 清空所选会话 |  |
| streak | 续火文字 |  |
| streak | 每天触发时间（0–23 时） |  |
| streak | 仅 Wi-Fi 可用时发送 |  |
| webdav_connection | 账号 |  |
| webdav_connection | 密码 / 应用专用密码 |  |
| webdav_connection | 允许明文 HTTP |  |
| streak | 自动续火设置 |  |
| webdav_connection | WebDAV 连接配置 |  |
| background_video | 视频背景 |  |
| background_video | 视频背景 · 启用视频背景 | 启用视频背景 |
| background_video | 视频背景 · 背景颜色 | 背景色 #RRGGBB / #AARRGGBB，留空为黑色 |
| background_video | 视频背景 · 选择背景图片 | 选择背景图片 |
| background_video | 视频背景 · 清除背景图片 | 清除背景图片 |
| background_video | 视频背景 · 图片缩放 | 图片缩放 |
| background_video | 视频背景 · 透明度 | 视频背景 · 不透明度 |
| background_photo | 图文背景 |  |
| background_photo | 图文背景 · 启用图文背景 | 启用图文背景 |
| background_photo | 图文背景 · 背景颜色 | 背景色 #RRGGBB / #AARRGGBB，留空为黑色 |
| background_photo | 图文背景 · 选择背景图片 | 选择背景图片 |
| background_photo | 图文背景 · 清除背景图片 | 清除背景图片 |
| background_photo | 图文背景 · 图片缩放 | 图片缩放 |
| background_photo | 图文背景 · 透明度 | 图文背景 · 不透明度 |
| background_livephoto | 实况背景 |  |
| background_livephoto | 实况背景 · 启用实况背景 | 启用实况背景 |
| background_livephoto | 实况背景 · 背景颜色 | 背景色 #RRGGBB / #AARRGGBB，留空为黑色 |
| background_livephoto | 实况背景 · 选择背景图片 | 选择背景图片 |
| background_livephoto | 实况背景 · 清除背景图片 | 清除背景图片 |
| background_livephoto | 实况背景 · 图片缩放 | 图片缩放 |
| background_livephoto | 实况背景 · 透明度 | 实况背景 · 不透明度 |
| home | 首页整理 |  |
| controls | 视频控件 |  |
| background | 播放背景 |  |
| clean | 清爽模式 |  |
| post_info | 作品信息显示 |  |
| miniapp | 小游戏免广告领奖 | 领奖<br>去广告<br>小游戏 |
| playlet_ad | 短剧广告 | 短剧<br>看广告解锁 |
| playlet_ad | 跳过短剧广告 | 短剧跳广告<br>短剧去广告<br>短剧插播广告<br>跳过短剧激励广告<br>看广告解锁 |
| skipad | 跳过短剧广告 |  |
| gestures | 手势设置 |  |
| longpress_playback | 长按播放 |  |
| autoplay | 自动连播 | 自动下滑<br>视频定时<br>图文停留<br>文章停留<br>完成延迟<br>触摸重新计时<br>评论区中允许下滑 |
| danmaku | 弹幕增强 | 重复弹幕扫描<br>自动扫描当前作品 |
| comments | 评论增强 |  |
| comment_time | 评论时间格式与模板 |  |
| comment_interaction | 评论交互 |  |
| comment_pages | 评论分页隐藏 |  |
| comment_appearance | 隐藏评论昵称 |  |
| comment_appearance | 隐藏评论正文文字 |  |
| comment_appearance | 隐藏评论时间 |  |
| comment_appearance | 隐藏评论IP 属地 |  |
| comment_appearance | 隐藏评论横向分割线 |  |
| comment_appearance | 隐藏评论标题竖线 |  |
| comment_appearance | 评论整区透明度 | 评论整区不透明度 |
| comment_appearance | 评论区外观 |  |
| comment_appearance | 隐藏评论装饰水印 |  |
| comment_menu | 评论长按菜单 |  |
| comment_filter | 评论过滤 |  |
| privacy | 隐私与状态 |  |
| privacy | 拦截在线状态上报 | 隐身<br>在线状态 |
| profile_style | 个人主页作品视图 |  |
| live_clean | 直播清爽 |  |
| search | 搜索页整理 |  |
| feed_filter | 内容过滤 |  |
| longpress_menu | 长按菜单隐藏项 |  |
| longpress_tools | 长按菜单工具 |  |
| token | 打开分享口令 |  |
| diagnostics | 兼容性检查 | 清除适配缓存 |
| logs | 运行日志 |  |
| bookmarks | 我的插眼 | 评论新回复<br>回复提醒<br>监测评论 |
| config | 配置备份与恢复 | 备份<br>恢复<br>插眼 |
| webdav | WebDAV备份与恢复 | 备份<br>恢复<br>插眼 |
| controls | 详情页控件外观 | 主页视频透明度<br>详情搜索透明度<br>评论栏透明度 |
| controls | 右侧不感兴趣按钮 |  |
| controls | 右侧连播按钮 |  |
| profile_style | 个人主页作品视图 |  |
| clean | 选择控件 |  |
| clean | 手势设置 |  |
| post_info | 居中显示信息窗口 |  |
| post_info | 发布地点 |  |
| post_info | 发布时间 |  |
| post_info | 视频时长 |  |
| post_info | 互动统计 |  |
| post_info | 视频下载 |  |
| post_info | 封面图片 |  |
| post_info | 当前图片 |  |
| post_info | 音乐信息 |  |
| post_info | 作品文案 |  |
| post_info | 复制文案按钮 |  |
| speed | 默认倍速 |  |
| speed | 记忆手动选择的倍速 |  |
| speed | 自定义长按倍速 |  |
| post_bar | 作品信息条 | 发布时间<br>发布地点<br>作品时间 |
| post_bar | 显示作品信息条 | 发布地点 |
| post_bar | 作品时间格式 | 发布时间 |
| post_bar | 作品信息模板 | 发布地点 |
| post_bar | 信息条字号（sp） | 作品时间 |
| post_bar | 信息条颜色 | 作品时间 |
| miniapp | 免广告领取奖励 | 领奖<br>去广告<br>跳广告<br>激励广告<br>小程序 |
| preload | 预加载策略 |  |
| proxy | 原画本地代理 |  |
| longpress_playback | 禁用新版长按播放控制模式 |  |
| autoplay | 连播方式 |  |
| search.642083e6c9 | 搜索页过滤 |  |
| search.5ce1e5947f | 隐藏搜索页右下角 AI 抖音 |  |
| search.2c5104df81 | 隐藏设置页 AI 搜索 |  |
| feed_filter.f42350811b | 启用内容过滤 |  |
| feed_filter.df653a2bf7 | 文案关键词 |  |
| feed_filter.regex | 文案正则 | 正则过滤<br>逐条规则 |
| feed_filter.scope | 仅过滤推荐流 | 仅推荐流 |
| feed_filter.2cb429cc12 | 屏蔽作者 |  |
| feed_filter.55dec76d5c | 作者白名单 |  |
| feed_filter.9e9332deda | 其他过滤设置 |  |
| feed_thresholds.a2065cac44 | 最低点赞数 |  |
| feed_thresholds.min_duration | 最短视频时长（秒） | 最短时长 |
| feed_thresholds.max_duration | 最长视频时长（秒） | 最长时长 |
| feed_thresholds.d2fc778560 | 最低评论数 |  |
| feed_thresholds.6ae0300933 | 最低收藏数 |  |
| feed_thresholds.e885e7e1b6 | 最低分享数 |  |
| feed_thresholds.61b4880c99 | 仅保留最近几天 |  |
| share.native | 隐藏原生分享项目 | 转发到日常<br>分享项目 |
| share.8db1c99607 | 隐藏原生好友列表 |  |
| share.27d06f49fc | 隐藏私信分享成功提示 |  |
| longpress_tools | 添加工具 |  |
| config | 从文件恢复 |  |
| config | 撤销最近一次恢复 |  |
| config | 导出范围 |  |
| webdav | 连接配置 |  |
| webdav | 上传当前插眼 |  |
| webdav | 查看远端备份 / 测试连接 |  |
| speed | 倍速 |  |
| speed | 长按倍速 |  |
| comment_time | 日期格式 |  |
| comment_time | 显示模板 | 时间模板 |
| page.extras | 画质与缓存 |  |
| speed | 自定义长按倍速 | 长按倍速 |
| playback_strategy | 播放完成后暂停 | 阻止合集与短剧自动下一集 |
| page.autoplay | 自动连播与完成动作 |  |
| speed | 长按倍速 |  |
