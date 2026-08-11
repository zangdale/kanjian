.PHONY: help build release install install-release clean assemble test lint

GRADLEW := ./gradlew

# AGP 8.x 需要 Java 17；优先使用 Homebrew openjdk@17
JAVA_17_HOME ?= /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export JAVA_HOME := $(JAVA_17_HOME)
export PATH := $(JAVA_HOME)/bin:$(PATH)

help:
	@echo "可用命令:"
	@echo "  make build            - 编译 Debug APK"
	@echo "  make release          - 编译 Release APK"
	@echo "  make install          - 安装 Debug 包到已连接设备"
	@echo "  make install-release  - 安装 Release 包到已连接设备"
	@echo "  make assemble         - 编译 Debug 与 Release"
	@echo "  make test             - 运行单元测试"
	@echo "  make lint             - 运行 Android Lint"
	@echo "  make clean            - 清理构建产物"
	@echo ""
	@echo "当前 JAVA_HOME=$(JAVA_HOME)"
	@java -version 2>&1 | head -1

build:
	$(GRADLEW) assembleDebug

release:
	$(GRADLEW) assembleRelease

install:
	$(GRADLEW) installDebug

install-release:
	$(GRADLEW) installRelease

assemble:
	$(GRADLEW) assemble

test:
	$(GRADLEW) test

lint:
	$(GRADLEW) lint

clean:
	$(GRADLEW) clean
