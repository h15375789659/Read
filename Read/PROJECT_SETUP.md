# 项目设置说明

## Java版本要求

本项目需要 **Java 21** 来编译和运行。

### 配置Java 21

#### 方法1：设置JAVA_HOME环境变量（推荐）
在系统环境变量中设置JAVA_HOME指向Java 21的安装路径：
```
JAVA_HOME=C:\Program Files\Java\jdk-21
```

#### 方法2：在local.properties中指定Java路径
在项目根目录的`local.properties`文件中添加：
```properties
org.gradle.java.home=C:\\Program Files\\Java\\jdk-21
```

#### 方法3：在Android Studio中配置
1. 打开 File -> Settings -> Build, Execution, Deployment -> Build Tools -> Gradle
2. 在 "Gradle JDK" 下拉菜单中选择 Java 21

### 验证Java版本
运行以下命令验证Java版本：
```bash
java -version
```

应该显示类似：
```
java version "21.0.x"
```

## 已配置的依赖

项目已配置以下主要依赖：

### 核心框架
- **Hilt 2.50** - 依赖注入
- **Room 2.6.1** - 本地数据库
- **Retrofit 2.9.0** - 网络请求
- **RxJava 3.1.8** - 异步处理
- **Jsoup 1.17.2** - HTML解析
- **MPAndroidChart 3.1.0** - 图表库

### 测试框架
- **JUnit 4.13.2** - 单元测试
- **jqwik 1.8.2** - 属性测试（Property-Based Testing）
- **Espresso 3.5.1** - UI测试

## 项目结构

```
app/src/main/java/com/example/read/
├── data/           # 数据层（Repository实现、数据源、Entity）
├── domain/         # 领域层（领域模型、Repository接口）
├── presentation/   # 表现层（Activity、Fragment、ViewModel）
├── di/             # 依赖注入模块
└── utils/          # 工具类
```

## Hilt配置

项目已启用Hilt依赖注入：
- `NovelReaderApplication` - 应用类，使用 `@HiltAndroidApp` 注解
- `MainActivity` - 使用 `@AndroidEntryPoint` 注解
- `AppModule` - 提供应用级别的依赖
- `DatabaseModule` - 数据库相关依赖（待实现）
- `NetworkModule` - 网络相关依赖（待实现）

## 权限配置

AndroidManifest.xml中已添加以下权限：
- `INTERNET` - 网络访问
- `ACCESS_NETWORK_STATE` - 网络状态检查
- `READ_EXTERNAL_STORAGE` - 读取外部存储（API 32及以下）
- `READ_MEDIA_DOCUMENTS` - 读取文档（API 33及以上）

## 下一步

配置好Java 21后，运行以下命令同步项目：
```bash
./gradlew clean build
```

或在Android Studio中点击 "Sync Project with Gradle Files"。
