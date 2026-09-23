# LangGraph4j Voice Guide

语音转写后的简历内容整理与 AI 分析服务。服务默认监听 `8093`，运行配置通过环境变量注入，密钥和运行期语音资产不进入 Git。

## Repository

- GitHub: `https://github.com/cchunH/LangGraph4jVoiceGuide.git`
- Local workspace: `D:\ProjectSpace\jmmt\v2.0\LangGraph4jVoiceGuide`
- Production workspace: `/opt/interview-elf/LangGraph4jVoiceGuide`
- Default branch: `main`

## Standard update flow

1. 在本地完成修改并运行 `mvn test`。
2. 提交并推送到 `origin/main`。
3. 在生产目录执行 `git pull --ff-only origin main`。
4. 按 `/opt/interview-elf` 的 Docker Compose 配置重建对应服务。

不要再通过直接覆盖源码目录作为日常发布方式；如需紧急传输文件，也应随后将同一修改补交到 Git，保证本地、GitHub 和线上三方一致。

## Runtime configuration

阿里云 DashScope、FunASR 等地址和密钥由部署环境变量提供。`.env*`、`voice-assets/`、`target/` 及服务器维护备份均已从 Git 排除。

