## 接入LangChain4j

### 1.引入官方的依赖:

~~~properties
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j</artifactId>
            <version>1.1.0</version>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
            <version>1.1.0-beta7</version>
        </dependency>


~~~

### 2.写一个接口，用来注入提示词

~~~java
package com.yosh.coding.artificalIntelligence;

import com.yosh.coding.artificalIntelligence.model.HtmlCodeResult;
import com.yosh.coding.artificalIntelligence.model.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;

public interface AiCodeGeneratorService {

    @SystemMessage(fromResource = "prompt/code-one-file-html.txt")
    HtmlCodeResult generateCode(String userMessage);


    @SystemMessage(fromResource = "prompt/code-multi-file-html.txt")
    MultiFileCodeResult generateMultiCode(String userMessage);
}

~~~

这里的返回对象是自定义的，返回的json,看你想让他们返回什么形式，毕竟万物皆对象，使用Description描述，可以让AI识别更加精准

#### 返回对象

~~~java
package com.yosh.coding.artificalIntelligence.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
@Description("this is result of that create HTML!")
public class HtmlCodeResult {

    @Description("HTML code")
    private String htmlCode;

    @Description("description your create HTML code")
    private String description;
}

~~~

~~~java
package com.yosh.coding.artificalIntelligence.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
@Description("this is result of that create HTML,Js and CSS!")
public class MultiFileCodeResult {
    @Description("this is result of that create HTML!")
    private String htmlCode;
    @Description("this is result of that create css!")
    private String cssCode;
    @Description("this is result of that create js!")
    private String jsCode;
    @Description("description your create code!")
    private String description;
}

~~~

#### 提示词

~~~tex
请使用原生 HTML、CSS、JavaScript 生成一个完整网页，采用单文件结构。

要求：
1. 只生成一个文件：index.html。
2. HTML、CSS、JavaScript 全部写在 index.html 里面。
3. CSS 写在 <style> 标签中。
4. JavaScript 写在 <script> 标签中。
5. 不允许使用 Vue、React、jQuery、Bootstrap、Tailwind 等框架或第三方库。
6. 页面可以直接双击 index.html 运行。
7. 网页主题是：【填写你的网页主题】。

页面内容要求：
1. 顶部导航栏。
2. 首页 Banner 区域。
3. 内容介绍区域。
4. 卡片展示区域。
5. 表单提交区域。
6. 底部版权区域。

交互要求：
1. 使用 JavaScript 实现导航点击效果。
2. 使用 JavaScript 实现表单校验。
3. 使用 JavaScript 实现按钮点击提示。
4. 使用 JavaScript 实现简单的数据渲染。

样式要求：
1. 页面美观、整洁、现代化。
2. 适配电脑和手机。
3. 按钮、卡片、表单要有美化。
4. 鼠标悬停要有简单动画。
5. 配色统一，布局清晰。

输出要求：
1. 直接输出完整的 index.html 代码。
2. 不要省略任何代码。
3. 不要只写说明。
4. 代码中添加必要注释。
~~~

~~~tex
请使用原生 HTML、CSS、JavaScript 生成一个完整网页，采用三文件结构。

要求：
1. 只生成三个文件：
   - index.html
   - style.css
   - script.js

2. index.html 只负责网页结构。
3. style.css 只负责网页样式。
4. script.js 只负责网页交互逻辑。
5. 不要创建文件夹。
6. 不要使用多级目录。
7. 不允许使用 Vue、React、jQuery、Bootstrap、Tailwind 等框架或第三方库。
8. 三个文件放在同一个位置后，双击 index.html 可以直接运行。
9. 网页主题是：【填写你的网页主题】。

index.html 要求：
1. 引入 style.css。
2. 引入 script.js。
3. 包含顶部导航栏。
4. 包含首页 Banner 区域。
5. 包含内容介绍区域。
6. 包含卡片展示区域。
7. 包含表单提交区域。
8. 包含底部版权区域。

style.css 要求：
1. 页面整体美观、整洁、现代化。
2. 使用响应式布局，适配电脑和手机。
3. 统一配色。
4. 美化导航栏、按钮、卡片、表单、页脚。
5. 鼠标悬停要有简单动画效果。

script.js 要求：
1. 实现导航点击效果。
2. 实现表单输入校验。
3. 实现按钮点击提示。
4. 实现数据列表或卡片的动态渲染。
5. 使用原生 JavaScript，不使用任何第三方库。
6. 代码要有必要注释。

输出要求：
1. 按下面格式分别输出三个文件的完整代码：

【index.html】
这里写完整 HTML 代码

【style.css】
这里写完整 CSS 代码

【script.js】
这里写完整 JavaScript 代码

2. 不要省略任何代码。
3. 不要只写说明。
4. 保证三个文件放在同一个位置后可以直接运行。
~~~



### 3.制作一个工厂创建Bean

~~~java
package com.yosh.coding.artificalIntelligence.config;

import com.yosh.coding.artificalIntelligence.AiCodeGeneratorService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIInitConfig {
    @Resource
    private ChatModel dskChatModel;
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService(){
        return AiServices.create(AiCodeGeneratorService.class,dskChatModel);
    }

}

~~~

这里需要注入ChatModel，注入模型，然后创建AiCodeGeneratorService的Bean.然后返回AiServices.create的方法，传入刚才的接口和模型

### 4.填写配置

~~~properties
langchain4j:
  open-ai:
    chat-model:
      base-url: https://api.deepseek.com
      api-key: ####
      model-name: deepseek-chat
      log-requests: true
      log-responses: true
      max-tokens: 8192

~~~

注意这里的最大token不要超过官网的要求.

### 5.调用

~~~java
package com.yosh.coding;

import com.yosh.coding.artificalIntelligence.AiCodeGeneratorService;
import com.yosh.coding.artificalIntelligence.model.HtmlCodeResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class AIChatTest {
    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    public void chat()
    {
        HtmlCodeResult s = aiCodeGeneratorService.generateCode("我是苹果");
        System.out.println(s);
    }
}

~~~

