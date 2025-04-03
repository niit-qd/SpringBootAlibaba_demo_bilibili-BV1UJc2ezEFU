[TOC]

---

### `OpenFeign`的`Logger`配置

**参考**
- [Overriding Feign Defaults](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html#spring-cloud-feign-overriding-defaults)
- [Feign logging](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html#feign-logging)

1. `Feign logging`
    > A logger is created for each Feign client created. By default, the name of the logger is the full class name of the interface used to create the Feign client. Feign logging only responds to the `DEBUG` level.
    `OpenFeign`默认只支持`DEBUG`级别的日志。
    > *`application.yml`*
    > ``` yaml
    > logging.level.project.user.UserClient: DEBUG
    > ```

    上面的前半部分是`logging.level.`固定格式。后半部分是包名`project.user`或者类路径`project.user.UserClient`。

    除此之外，还要指定`Logger.Level`。例如，下面的例子设置`Logger.Level`为`FULL`：
    ``` Java
    @Configuration
    public class FooConfiguration {
        @Bean
        Logger.Level feignLoggerLevel() {
            return Logger.Level.FULL;
        }
    }
    ```

    `Logger.Level`默认为`NONE`。

    > The `Logger.Level` object that you may configure per client, tells Feign how much to log. Choices are:
    > - `NONE`, No logging (**DEFAULT**).
    > - `BASIC`, Log only the request method and URL and the response status code and execution time.
    > - `HEADERS`, Log the basic information along with request and response headers.
    > - `FULL`, Log the headers, body, and metadata for both requests and responses.


2. 自定义OpenFeign的`Logger`
    > Spring Cloud creates a new ensemble as an `ApplicationContext` on demand for each named client using `FeignClientsConfiguration`. 

    Spring Cloud为每一个client使用了一个名为`FeignClientsConfiguration`的代理类。
    可以修改该配置类的`@Bean`来`Override`某些特性。
    > Spring Cloud OpenFeign provides the following beans by default for feign (`BeanType` beanName: `ClassName`):
    > - `Logger` feignLogger: `Slf4jLogger`
    > - ...

    `Spring Cloud OpenFeign`默认使用`Slf4jLogger`作为`feign.Logger`的实现。可以查看`FeignClientsConfiguration`的默认配置：
    ``` Java
    @Configuration(proxyBeanMethods = false)
    public class FeignClientsConfiguration {
        @Autowired(required = false)
        private Logger logger;

        @Bean
        @ConditionalOnMissingBean(FeignLoggerFactory.class)
        public FeignLoggerFactory feignLoggerFactory() {
            return new DefaultFeignLoggerFactory(logger);
        }
    ```
    ``` Java
    public class DefaultFeignLoggerFactory implements FeignLoggerFactory {

        private final Logger logger;

        public DefaultFeignLoggerFactory(Logger logger) {
            this.logger = logger;
        }

        @Override
        public Logger create(Class<?> type) {
            return this.logger != null ? this.logger : new Slf4jLogger(type);
        }

    }
    ```

    另外在`Slf4jLogger`的定义中，只有在`DEBUG`模式时才会执行输出，所以，`OpenFeign`默认只支持`DEBUG`级别的日志。同时`Slf4jLogger`使用的logger实现是`org.slf4j.Logger`。
    ``` Java
    public class Slf4jLogger extends feign.Logger {
      private final Logger logger;
      @Override
      protected void log(String configKey, String format, Object... args) {
        // Not using SLF4J's support for parameterized messages (even though it would be more efficient)
        // because it would
        // require the incoming message formats to be SLF4J-specific.
        if (logger.isDebugEnabled()) {
          logger.debug(String.format(methodTag(configKey) + format, args));
        }
      }
    }   
    ```

    > Spring Cloud OpenFeign *does not* provide the following beans by default for feign, but still looks up beans of these types from the application context to create the Feign client:
    > - `Logger.Level`
    > - ...

    尽管Spring Cloud OpenFeign默认不为feigh提供部分bean，但是依然可以查找这些bean。
    例如`Logger.Level`。

    **示例：**
    1. 重写`FeignClientsConfiguration`，替换`Logger`的实现类，并配置`Logger`级别。
    如下，其level是被设置为`FINE`，并配置了logger内容的写入文件。
    注：`JavaLogger`，仅供参考，用处不大。
        ``` Java
        @Configuration(proxyBeanMethods = false)
        public class MyFeignClientsConfiguration {
            @Bean
            Logger feignLogger() {
                Logger.JavaLogger logger = new Logger.JavaLogger();
                // 下面的步骤会设置logger的level为FINE，并将logger内容写入到指定的文件
                File dir = new File("target");
                if (!dir.exists() && !dir.isDirectory()) {
                    try {
                        dir.mkdirs();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (dir.isDirectory()) {
                    logger.appendToFile(dir.getAbsolutePath() + File.separator + "JavaLogger.log");
                }
                return logger;
            }
            @Bean
            Logger.Level feignLoggerLevel() {
                return Logger.Level.FULL;
            }
        }
        ```
    注：参考示例`FeignClientsConfiguration`的默认配置，还是建议配置属性`proxyBeanMethods = false`。

3. 对个别的`FeignClient`使用单独的配置
    上面的自定义方式可以用于全局配置的情况。
    如果需要为某个`FeignClient`进行客制化配置，不要在配置类上再使用`@Configuration`。
    处理方法是使用`FeignClient`的`configuration`属性。
    ``` Java
    @Configuration
    public class FooConfiguration {
        ...
    }
    ```
    ``` Java
    @FeignClient(contextId = "fooClient", name = "stores", configuration = FooConfiguration.class)
    public interface FooClient {
    	//..
    }
    ```

    **示例：**
    1. 自定义`Logger.Level`
        ``` Java
        @Configuration
        public class FooConfiguration {
            @Bean
            public Logger.Level feignLoggerLevel() {
                return Logger.Level.FULL;
            }
        }
        ```
        ``` Java
        @FeignClient(contextId = "fooClient", name = "stores", configuration = FooConfiguration.    class)
        public interface FooClient {
        	//..
        }
        ```
    2. 自定义`Logger`为：`Slf4jLogger`
        `Slf4jLogger`基于`org.slf4j.Logger`，需要设置`logger-name`，并配置`level`。
        创建`Slf4jLogger`，直接`new`即可：`new Logger(${logger-name})`。
        一般情况下，都会使用源类名作为`logger-name`。
        > [Logging](https://docs.spring.io/spring-boot/reference/features/logging.html)
        > Logger name: This is usually the source class name (often abbreviated).
        > [Log Levels](https://docs.spring.io/spring-boot/reference/features/logging.html#features.logging.log-levels)
        > All the supported logging systems can have the logger levels set in the Spring [Environment](https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/core/env/Environment.html) (for example, in `application.properties`) by using `logging.level.<logger-name>=<level>` where level is one of TRACE, DEBUG, INFO, WARN, ERROR, FATAL, or OFF. The root logger can be configured by using `logging.level.root`.

        由于无法通过一个`class`类型为配置类设置源类相关的参数，所以，较好的办法是将配置类设置为当前类的一个静态内部类。
        ``` Java
        @FeignClient(name = "fooClient", configuration = FooClient.FooConfiguration.class)
        public interface FooClient {

            static final class FooConfiguration<T> {
                @Bean(name = "feignLogger")
                Logger feignLogger() {
                    new Slf4jLogger(ProductFeignClient.class);
                    return new Slf4jLogger();
                }

                @Bean
                public Logger.Level feignLoggerLevel() {
                    return Logger.Level.FULL;
                }
            }

            @GetMapping("/get/{id}")
            Product getProduct(@PathVariable("id") Long productId);

        }
        ```
