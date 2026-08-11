# Kanjian（看见）

<p float="left" align="center">
    <img src="app/src/main/res/drawable/kanjian_logo.png" width="120" alt="Kanjian logo" />
</p>

看见（Kanjian）可在后台持续录制音频/视频，并在需要时保存最近 30 分钟的内容。  
所有选项均可自定义配置，无需网络连接。

## 功能

- 持续音频与视频录制
- 按需保存录音/录像
- 可配置时长与画质
- 可选视频水印（时间戳与 GPS 坐标）

## 构建

项目使用 Gradle Wrapper，也可通过 Makefile 快捷调用：

```bash
make help      # 查看可用命令
make build     # 编译 Debug APK
make release   # 编译 Release APK
make install   # 安装 Debug 包到设备
make clean     # 清理构建产物
```
