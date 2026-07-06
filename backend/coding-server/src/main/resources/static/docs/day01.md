##  如何实现AOP

### 1.引入依赖

~~~properties
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
~~~

### 2.引入注解

~~~java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserLoginAnnotation {

}

~~~

注解:



#### `@Target(ElementType.METHOD)`

指定这个注解**只能加在方法上**。

`ElementType` 的其他选项：

| 值                | 能加在哪里       |
| ----------------- | ---------------- |
| `METHOD`          | 方法             |
| `TYPE`            | 类/接口/枚举     |
| `FIELD`           | 字段             |
| `PARAMETER`       | 方法参数         |
| `ANNOTATION_TYPE` | 注解上（元注解） |

可以同时指定多个：
```java
@Target({ElementType.METHOD, ElementType.TYPE})
```

---

#### `@Retention(RetentionPolicy.RUNTIME)`

指定注解的**生命周期**，即保留到什么阶段。

| 值        | 含义                                           |
| --------- | ---------------------------------------------- |
| `SOURCE`  | 只在源码阶段存在，编译后丢弃（如 `@Override`） |
| `CLASS`   | 保留到 `.class` 文件，运行时不可见（默认值）   |
| `RUNTIME` | **保留到运行时**，可通过反射读取               |

AOP 和 Spring 注解必须用 `RUNTIME`，否则运行时切面找不到它。

---

#### `public @interface UserLoginAnnotation`

`@interface` 是定义注解的关键字，本质上是一个特殊的接口。

可以给注解加属性：
```java
public @interface UserLoginAnnotation {
    String value() default "";   // 有默认值，使用时可不传
    boolean required() default true;
}
```

使用时：
```java
@UserLoginAnnotation(value = "管理员接口", required = true)
public BaseResponse<?> adminApi() { ... }
```

### 3.切面类

 前置条件注入 @Aspect

#### 1.定义切点，名称任意

~~~java
 @Pointcut("@annotation(com.yosh.coding.annotation.AuthCheck)")
    public  void point(){}
~~~

这里@annotation后面是你想切入哪个地方后续可以用

#### 2.调用

~~~java
    @Around("point()")
    public Object doBefore(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        //获取被拦截方法的签名信息 ----> Signature
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        // 通过Method反射拿到@AuthCheck对象的实例
        AuthCheck authCheck = signature.getMethod().getAnnotation(AuthCheck.class);
        // 获取用户权限
        String role = authCheck.mustRole();
        if(StrUtil.isBlank(role)) return proceedingJoinPoint.proceed();
        // 校验权限
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = servletRequestAttributes.getRequest();

        BaseResponse<LoginUserVO> loginUser = userService.getLoginUser(request);
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getData().getUserRole());
        if(userRoleEnum == null) throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        
        if(!role.equals(userRoleEnum.getValue())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        
        return proceedingJoinPoint.proceed();

    }
~~~



## 如何隐藏秘钥信息

### 1.引入依赖

~~~properties
<dependency>
    <groupId>me.paulschwarz</groupId>
    <artifactId>spring-dotenv</artifactId>
    <version>4.0.0</version>
</dependency>

~~~

### 2.撰写.env

~~~properties
#mysql config
MYSQL_URL=47.99.125.14
MYSQL_PORT=33099
MYSQL_DATABASE=yu_ai_code_mother
MYSQL_ACCOUNT=root
MYSQL_PASSWORD=147258369

#redis config
REDIS_URL=47.99.125.14
REDIS_PORT=4406
REDIS_PASSWORD=123456
REDIS_DB=1

~~~



### 3.忽略.env于gitignore



## Mybatis-Fliex生成代码

### 1.构建.env

### 2.在测试类下构建文件

~~~java
package com.yosh.coding;

import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.ColumnConfig;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.yosh.common.PageResquest;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class Codegen {

    @Value("${MYSQL_URL}")
    private String URL;
    @Value("${MYSQL_PORT}")
    private String PORT;
    @Value("${MYSQL_ACCOUNT}")
    private String account;
    @Value("${MYSQL_PASSWORD}")
    private String  password;
    @Value("${MYSQL_DATABASE}")
    private String database;
    private static final String[] TABLE_NAMES = {"user"};
    @Test
    public  void test() {
        //配置数据源
        HikariDataSource dataSource = new HikariDataSource();
        String LINK_URL = "jdbc:mysql://" + URL + ":" + PORT + "/" + database + "?characterEncoding=utf-8";
        dataSource.setJdbcUrl(LINK_URL);
        dataSource.setUsername(account);
        dataSource.setPassword(password);

        //创建配置内容，两种风格都可以。
        GlobalConfig globalConfig = createGlobalConfigUseStyle1();

        //通过 datasource 和 globalConfig 创建代码生成器
        Generator generator = new Generator(dataSource, globalConfig);

        //生成代码
        generator.generate();
    }

    public static GlobalConfig createGlobalConfigUseStyle1() {
        // 创建配置内容
        GlobalConfig globalConfig = new GlobalConfig();

        // 设置根包，建议先生成到一个临时目录下，生成代码后，再移动到项目目录下
        globalConfig.getPackageConfig()
                .setBasePackage("com.yosh.genresult");

        // 设置表前缀和只生成哪些表，setGenerateTable 未配置时，生成所有表
        globalConfig.getStrategyConfig()
                .setGenerateTable(TABLE_NAMES)
                // 设置逻辑删除的默认字段名称
                .setLogicDeleteColumn("isDelete");

        // 设置生成 entity 并启用 Lombok
        globalConfig.enableEntity()
                .setWithLombok(true)
                .setJdkVersion(21);

        // 设置生成 mapper
        globalConfig.enableMapper();
        globalConfig.enableMapperXml();

        // 设置生成 service
        globalConfig.enableService();
        globalConfig.enableServiceImpl();

        // 设置生成 controller
        globalConfig.enableController();

        return globalConfig;
    }

}
~~~

## Serializable

Serializable 是一个标记接口（marker interface）——它没有任何方法，只是给类打个"标签"，告诉 JVM：这个类的对象可以被序列化。

序列化是干嘛的
序列化：把内存里的 Java 对象转成字节流（一串二进制），以便：

存到磁盘（持久化）
通过网络传输（RPC、分布式调用）
存进 Redis / 缓存
反序列化：把字节流再还原成 Java 对象。

对象（内存）  --序列化-->  字节流 0101...  --反序列化-->  对象（内存）

为什么 BaseResponse 要实现它
BaseResponse 是接口返回结果，可能会被：

存进  Redis  缓存
在分布式服务间传递
实现 Serializable 是一种习惯性的保险，保证它在任何需要序列化的场景都不会出问题。