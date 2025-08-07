import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.3.0"
}

group = "com.ljh.request"
version = "1.0.1"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html

dependencies {
    intellijPlatform {
        create(
            providers.gradleProperty("platformType"),
            providers.gradleProperty("platformVersion"),
            true
        )
            bundledPlugin("com.intellij.java")

    }
    implementation("cn.hutool:hutool-all:5.8.26")
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    implementation("com.fifesoft:rsyntaxtextarea:3.3.3")
}

intellijPlatform {
    pluginConfiguration {
        name = "RequestMan"
        version = "1.0.1"
        description = """
            <b>RequestMan</b> - A powerful API testing and documentation generation tool integrated into IntelliJ IDEA. Supports both "API scanning mode" and "custom API mode" for efficient development and API management.<br>
            <br>
            <b>RequestMan</b> - 一款集成于 IntelliJ IDEA 的接口调试与文档生成工具，支持"接口扫描模式"与"自定义接口模式"自由切换，助力高效开发与接口管理。<br>
            <br>
            <b>核心功能：</b>
            <ul>
              <li><b>智能接口扫描：</b> 一键扫描项目中的 Spring 接口（<code>@RestController</code>、<code>@RequestMapping</code>），自动生成接口列表、参数结构，支持文档预览与 JSON 示例生成</li>
              <li><b>自定义接口管理：</b> 手动添加、编辑、保存自定义 API，支持多种请求体类型、认证、后置操作等高级功能</li>
              <li><b>环境与变量管理：</b> 支持多环境配置，全局变量管理，使用 <code>{{变量名}}</code> 动态引用</li>
              <li><b>高效搜索定位：</b> 快捷键 <b>Ctrl+Alt+/</b> 一键搜索，支持按 URL、方法名、三方包等多维度检索</li>
              <li><b>响应处理与文档：</b> 响应折叠、JSON 格式化、文档预览、内置 JSONPath 提取器</li>
              <li><b>接口导入导出：</b> 支持接口集合的导入导出，使用标准 JSON 格式进行数据交换</li>
            </ul>
            <br>
            <b>快速开始：</b>
            <ol>
              <li><b>插件设置：</b> 在 IDEA 设置中选择 <b>RequestMan Settings</b>，配置环境管理和全局变量</li>
              <li><b>接口扫描：</b> 打开工具窗口，点击"刷新接口"自动扫描 Spring 接口，选择接口进行调试</li>
              <li><b>自定义接口：</b> 切换模式，点击"新增接口"创建自定义 API，支持多种 Body 类型和后置操作</li>
              <li><b>接口搜索：</b> 按 <b>Ctrl+Alt+/</b> 快速搜索和定位接口</li>
              <li><b>变量引用：</b> 在参数中使用 <code>{{变量名}}</code> 动态引用全局变量</li>
            </ol>
            <br>
        """.trimIndent()
        
        ideaVersion {
            sinceBuild = "251"
        }

        changeNotes = """
            <b>1.0.1</b> - API 兼容性与性能优化
            <ul>
              <li><b>API 兼容性：</b> 替换弃用 API 和实验性 API，使用稳定替代方案</li>
              <li><b>问题修复：</b> 修复 UI 组件中实验性 API 使用警告</li>
            </ul>
            
            <b>1.0.0</b> - 主要版本发布与代码优化
            <ul>
              <li><b>主要版本：</b> 稳定的 1.0.0 版本，包含完整功能集</li>
              <li><b>代码优化：</b> 重构和优化代码库，提升可维护性</li>
              <li><b>文档完善：</b> 增强插件文档和用户指南</li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
        options.compilerArgs.add("-Xlint:unchecked")
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }
}
