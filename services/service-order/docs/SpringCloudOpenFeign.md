[TOC]

---

### 参考
- [Overriding Feign Defaults](https://docs.spring.io/spring-cloud-openfeign/refespring-cloud-openfeign.html#spring-cloud-feign-overriding-defaults)
- [Feign logging](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html#feign-logging)
- [闲聊 DNS 系统中域名的格式标准：下划线“_”是被允许的吗？](https://ephen.me/2019/zone-format/)


### `OpenFeign`中`Client`在SpringContenx中的注册

- [Spring框架探秘：深入理解registerBeanDefinitions方法的奥秘](https://cloud.tencent.com/developer/article/2412421)
- [注解 @EnableFeignClients 工作原理](https://blog.csdn.net/andy_zhang2007/article/details/86680622)

1. `EnableFeignClients`注解
    `EnableFeignClients`中`import`了`FeignClientsRegistrar`:
    ``` Java
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Documented
    @Import(FeignClientsRegistrar.class)
    public @interface EnableFeignClients {
        ...
    }
    ```
    当然，该注解类还包含`basePackages`等组件扫描属性。这里不展开。
2. `FeignClientsRegistrar`注册bean
    `FeignClientsRegistrar`重写了`ImportBeanDefinitionRegistrar`的`registerBeanDefinitions`方法（默认空实现）。
    该方法完成对默认配置和FeignClient的配置。
    ``` Java
        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
            registerDefaultConfiguration(metadata, registry);
            registerFeignClients(metadata, registry);
        }
    ```
3. 默认配置类的配置
    从`defaultConfiguration`中读取默认配置类名，调用`registerClientConfiguration`注册默认配置。
    该默认配置的默认值是`FeignClientsConfiguration`。
    ``` Java
        private void registerDefaultConfiguration(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
            Map<String, Object> defaultAttrs = metadata.getAnnotationAttributes(EnableFeignClients.class.getName(), true);

            if (defaultAttrs != null && defaultAttrs.containsKey("defaultConfiguration")) {
                String name;
                if (metadata.hasEnclosingClass()) {
                    name = "default." + metadata.getEnclosingClassName();
                }
                else {
                    name = "default." + metadata.getClassName();
                }
                registerClientConfiguration(registry, name, "default", defaultAttrs.get("defaultConfiguration"));
            }
        }
    ```
    ``` Java
        private void registerClientConfiguration(BeanDefinitionRegistry registry, Object name, Object className,
                Object configuration) {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FeignClientSpecification.class);
            builder.addConstructorArgValue(name);
            builder.addConstructorArgValue(className);
            builder.addConstructorArgValue(configuration);
            registry.registerBeanDefinition(name + "." + FeignClientSpecification.class.getSimpleName(),
                    builder.getBeanDefinition());
        }
    ```
4. 注册`Client`
    这里通过`registerClientConfiguration`进行组件扫描，以`FeignClient`作为过滤器，将被`FeignClient`注解的类型进行注册。
    ``` Java
    	public void registerFeignClients(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
    		LinkedHashSet<BeanDefinition> candidateComponents = new LinkedHashSet<>();
            ...
            if (clients == null || clients.length == 0) {
                ClassPathScanningCandidateComponentProvider scanner = getScanner();
                scanner.setResourceLoader(this.resourceLoader);
                scanner.addIncludeFilter(new AnnotationTypeFilter(FeignClient.class));
                ...
            }
            ...
    		for (BeanDefinition candidateComponent : candidateComponents) {
    			if (candidateComponent instanceof AnnotatedBeanDefinition beanDefinition) {
                    ...
    				String name = getClientName(attributes);
    				String className = annotationMetadata.getClassName();
    				registerClientConfiguration(registry, name, className, attributes.get("configuration"));
    
    				registerFeignClient(registry, annotationMetadata, attributes);
    			}
    		}
    	}
    ```
    注册的名字来自于`getClientName`方法。
    可知，注册的`Client`的bean名称来是：`name + "." + FeignClientSpecification.class.getSimpleName()`。而`name`的取值来自`FeignClient`属性中`contextId`、`value`、`name`、`serviceId`*（已弃用）*中第一个非空值。
    ``` Java
        private String getClientName(Map<String, Object> client) {
            if (client == null) {
                return null;
            }
            String value = (String) client.get("contextId");
            if (!StringUtils.hasText(value)) {
                value = (String) client.get("value");
            }
            if (!StringUtils.hasText(value)) {
                value = (String) client.get("name");
            }
            if (!StringUtils.hasText(value)) {
                value = (String) client.get("serviceId");
            }
            if (StringUtils.hasText(value)) {
                return value;
            }

            throw new IllegalStateException(
                    "Either 'name' or 'value' must be provided in @" + FeignClient.class.getSimpleName());
        }
    ```

    If we want to create multiple Feign clients with the same name or url so that they would point to the same server but each with a different custom configuration then we have to use `contextId` attribute of the `@FeignClient` in order to avoid name collision of these configuration beans.
    如果我们想要创建具有相同名称或 URL 的多个 Fe​​ign 客户端，以便它们指向同一个服务器，但每个客户端都有不同的自定义配置，那么我们必须使用`contextId`属性`@FeignClient`来避免这些配置 bean 的名称冲突。
    示例：
    ``` Java
    @FeignClient(contextId = "fooClient", name = "stores", configuration = FooConfiguration.class)
    public interface FooClient {
        //..
    }
    ```
    ``` Java
    @FeignClient(contextId = "barClient", name = "stores", configuration = BarConfiguration.class)
    public interface BarClient {
        //..
    }
    ```
    个人习惯`contextId`的命名规则：`${name}-${path的变体}`。

5. 注解`FeignClient`部分属性取值
    **`org.springframework.cloud.openfeign.FeignClientsRegistrar`**
    1. `name`
        `serviceId` --> `name` --> `value`
        ``` Java
            String getName(ConfigurableBeanFactory beanFactory, Map<String, Object> attributes) {
                String name = (String) attributes.get("serviceId");
                if (!StringUtils.hasText(name)) {
                    name = (String) attributes.get("name");
                }
                if (!StringUtils.hasText(name)) {
                    name = (String) attributes.get("value");
                }
                name = resolve(beanFactory, name);
                return getName(name);
            }
        ```
        静态方法`getName`的作用是判断`name`值是否是一个有效。如果有效，返回原值；否则报异常。
    2. `contextId`
        `contextId`
        ``` Java
            // static String getName(String name) // 同上
            private String getContextId(ConfigurableBeanFactory beanFactory, Map<String, Object> attributes) {
                String contextId = (String) attributes.get("contextId");
                if (!StringUtils.hasText(contextId)) {
                    return getName(attributes);
                }

                contextId = resolve(beanFactory, contextId);
                return getName(contextId);
            }
        ```
        如果`contextId`为空，则使用`getName(attributes)`方法从属性中读取`serviceId`、`name`、`value`中第一个非空的值。
        静态方法`getName`的作用是判断`contextId`值是否是一个有效。如果有效，返回原值；否则报异常。
    3. `url`
        `url`
        ``` Java
            static String getUrl(String url) {
                if (StringUtils.hasText(url) && !(url.startsWith("#{") && url.contains("}"))) {
                    if (!url.contains("://")) {
                        url = "http://" + url;
                    }
                    try {
                        new URL(url);
                    }
                    catch (MalformedURLException e) {
                        throw new IllegalArgumentException(url + " is malformed", e);
                    }
                }
                return url;
            }
            private String getUrl(ConfigurableBeanFactory beanFactory, Map<String, Object> attributes) {
                String url = resolve(beanFactory, (String) attributes.get("url"));
                return getUrl(url);
            }
        ```
        如果非空，且没有以`"#{"`未开头以`"}"`结尾，如果没有包含`"://"`，则在`url`前追加`"http://"`。
        如果有效，返回原值；否则报异常。
    4. `url`
        `url`
        ``` Java
            static String getPath(String path) {
                if (StringUtils.hasText(path)) {
                    path = path.trim();
                    if (!path.startsWith("/")) {
                        path = "/" + path;
                    }
                    if (path.endsWith("/")) {
                        path = path.substring(0, path.length() - 1);
                    }
                }
                return path;
            }
            private String getPath(ConfigurableBeanFactory beanFactory, Map<String, Object> attributes) {
                String path = resolve(beanFactory, (String) attributes.get("path"));
                return getPath(path);
            }
        ```
        如果`path`没有以`/`开头，则在其前面追加。
        如果`path`以`/`结尾，则在其后面删除。
    5. `serviceId`和`contextId`的添加和移除
        1. **serviceId**
            移除别名：[Commit 880efd1](https://github.com/spring-cloud/spring-cloud-openfeign/commit/880efd18431f9299f41829aeda969d665357aa6f)
            ``` text
            Remove @AliasFor from @FeignClient.serviceId()
            The way it was being used internally means that we didn't need the
            annotation anyway. It would be nice to have it back if the Spring
            issue gets resolved, because then we could actually use it as
            intended.
            Fixes gh-1025
            ```
            > main v4.3.0-M2 v2.0.0.M1 880efd18431f9299f41829aeda969d665357aa6f

            移除：[Commit 5e61666](https://github.com/spring-cloud/spring-cloud-openfeign/commit/5e61666f7ca04e048ad34204b2a642fa3c257c7b)
            ``` Java
            Remove deprecations.
            ```
            > main v4.3.0-M2 v3.0.0-RC1 5e61666f7ca04e048ad34204b2a642fa3c257c7b

            | 版本                                   | `FeignClient`中是否存在性                    | `FeignClientsRegistrar`中是否存在                                                                                   |
            | -------------------------------------- | -------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
            | **v2.0.0.M1** 可以追溯的最早版本       | @deprecated use {@link #name() name} instead | 在`getClientName`方法中作为排在`value`和`name`之后的候选值                                                          |
            | **v2.2.10.RELEASE** 可以追溯的最后版本 | @deprecated use {@link #name() name} instead | 在`getClientName`方法中作为排在`value`和`name`之后的候选值                                                          |
            | **v3.0.0-M2**  可以追溯的首个移除版本  | 已经被移除                                   | 在`getClientName`方法中作为排在`value`和`name`之前的候选值；<br/>`serviceId`依然作为排在`value`和`name`之后的候选值 |
        
        2. **contextId**
            首次引入提交：[Commit e227808](https://github.com/spring-cloud/spring-cloud-openfeign/commit/e227808b9d47c8c35d2a60414cb1c83564e72e5c)
            ``` txt
            Support Multiple Clients Using The Same Service (#90)
            * Use serviceId as url target if present.
            Fixes gh-67
            * Code review fixes and improvements
            * Fix test
            * Add contextId to override bean name of feign client and its configuration.
            * Add documentation
            * Add contextId example to documentation
            ```
            > main(#90) v4.3.0-M2 v2.1.0.RELEASE  e227808b9d47c8c35d2a60414cb1c83564e72e5c

            | 版本                                  | `FeignClient`中是否存在性                                                                            | `FeignClientsRegistrar`中是否存在                                                                                   |
            | ------------------------------------- | ---------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
            | **v2.1.0.RELEASE** 可以追溯的最早版本 | This will be used as the bean name instead of name if present, but will not be used as a service id. | 在`getClientName`方法中作为排在`value`和`name`之前的候选值；<br/>`serviceId`依然作为排在`value`和`name`之后的候选值 |
            | **v2.2.10.RELEASE**                   | 同上                                                                                                 | 同上                                                                                                                |
            | **v3.0.0-M2**                         | 同上                                                                                                 | 同上                                                                                                                |
    6. 关于静态方法`static String getName(String name)`
        该方法的作用是用于判断给定字符串是否满足一个`host`语法。
        通过`URI(url).getHost()`返回`host`是否为`null`来判断给定字符串是否满足`host`语法。
        ``` Java
            static String getName(String name) {
                if (!StringUtils.hasText(name)) {
                    return "";
                }

                String host = null;
                try {
                    String url;
                    if (!name.startsWith("http://") && !name.startsWith("https://")) {
                        url = "http://" + name;
                    }
                    else {
                        url = name;
                    }
                    host = new URI(url).getHost();

                }
                catch (URISyntaxException ignored) {
                }
                Assert.state(host != null, "Service id not legal hostname (" + name + ")");
                return name;
            }
        ```
        与此相关的成员变量有：
        - `name`
        - `contextId`

        `host`语法要求：
        - [RFC-1034|RFC-1035]
            The labels must follow the rules for ARPANET host names. They must start with a letter, end with a letter or digit, and have as interior characters only letters, digits, and hyphen. There are also some restrictions on the length. Labels must be 63 characters or less.
            域名要遵循 ARPANET 的主机名格式：必须以字母开头、以字母或者数字结尾，中间部分为字母、数字或连字符，长度必须是 63 个字符或者更短。
        - [RFC 1101]
            The new syntax expands the set of names to allow leading digits, so long as the resulting representations do not conflict with IP addresses in decimal octet form.
            新的语法标准允许以数字开头，但结果不能与十进制八位字节形式的 IP 地址冲突。

        所以，`name`和`contextId`要满足`host`语法。例如不能包含下划线`_`，但是可以包含分隔符`-`和`.`。
        [闲聊 DNS 系统中域名的格式标准：下划线“_”是被允许的吗？](https://ephen.me/2019/zone-format/)

---

### 配置

---

#### 配置方式

1. 默认配置（已经指定了bean的名字）
    Spring Cloud creates a new ensemble as an ApplicationContext on demand for each named client using `FeignClientsConfiguration`. 
    Spring Cloud 使用`FeignClientsConfiguration`为每个命名客户端根据需要创建一个新的集合作为 ApplicationContext。
    即，只用使用`@FeignClient`注解，就会为对应命名的`Client`使用默认配置，该默认配置是`FeignClientsConfiguration`。
    Spring Cloud OpenFeign provides the following beans by default for feign (`BeanType` beanName: `ClassName`):
    - `Decoder` feignDecoder: `ResponseEntityDecoder` (which wraps a `SpringDecoder`)
    - `Encoder` feignEncoder: `SpringEncoder`
    - `Logger` feignLogger: `Slf4jLogger`
    - `MicrometerObservationCapability` micrometerObservationCapability: If `feign-micrometer` is on - the classpath and `ObservationRegistry` is available
    - MicrometerCapability micrometerCapability: If feign-micrometer is on the classpath, - MeterRegistry is available and ObservationRegistry is not available
    - `CachingCapability` cachingCapability: If `@EnableCaching` annotation is used. Can be disabled - via `spring.cloud.openfeign.cache.enabled`.
    - `Contract` feignContract: `SpringMvcContract`
    - `Feign.Builder` feignBuilder: `FeignCircuitBreaker.Builder`
    - `Client` feignClient: If Spring Cloud LoadBalancer is on the classpa`FeignBlockingLoadBalancerClient` is used. If none of them is on the classpath, the default Feign client is used.
      只有`Spring Cloud LoadBalancer`被启用的时候，`FeignBlockingLoadBalancerClient`才会被使用。所以，要在OpenFeign中启用负载均衡，需要引入`spring-cloud-loadbalancer`。这里使用其`starter`方式引入：`spring-cloud-starter-loadbalancer`。
2. OpenFeign未提供但也会查找的配置
    Spring Cloud OpenFeign does not provide the following beans by default for feign, but still looks up beans of these types from the application context to create the Feign client:
    - `Logger.Level`
    - `Retryer`
    - Error`Decoder
    - `Request.Options`
    - `Collection<RequestInterceptor>`
    - `SetterFactory`
    - `QueryMapEncoder`
    - `Capability` (`MicrometerObservationCapability` and `CachingCapability` are provided by default)
3. 通过注解`@FeignClient`的属性`configuration `来配置
    Spring Cloud lets you take full control of the Feign client by declaring additional configuration (on top of the `FeignClientsConfiguration`) using `@FeignClient`.
    
    1. 示例：
        ``` Java
        @Configuration
        public class FooConfiguration {

            @Bean
            public HttpClient5FeignConfiguration.HttpClientBuilderCustomizer httpClientBuilder() {
                return (httpClientBuilder) -> {
                    RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
                    requestConfigBuilder.setProtocolUpgradeEnabled(false);
                    httpClientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());
                };
            }
        }
        @FeignClient(name = "stores", configuration = FooConfiguration.class)
        public interface StoreClient {
            //..
        }
        ```
    
4. 通过配置文件来配置
    `@FeignClient` also can be configured using configuration properties.
    
    1. 示例：
        ``` yaml
        spring:
            cloud:
                openfeign:
                    client:
                        config:
                            feignName:
                                url: http://remote-service.com
                                connectTimeout: 5000
                                readTimeout: 5000
                                loggerLevel: full
                                errorDecoder: com.example.SimpleErrorDecoder
                                retryer: com.example.SimpleRetryer
                                defaultQueryParameters:
                                    query: queryValue
                                defaultRequestHeaders:
                                    header: headerValue
                                requestInterceptors:
                                    - com.example.FooRequestInterceptor
                                    - com.example.BarRequestInterceptor
                                responseInterceptor: com.example.BazResponseInterceptor
                                dismiss404: false
                                encoder: com.example.SimpleEncoder
                                decoder: com.example.SimpleDecoder
                                contract: com.example.SimpleContract
                                capabilities:
                                    - com.example.FooCapability
                                    - com.example.BarCapability
                                queryMapEncoder: com.example.SimpleQueryMapEncoder
                                micrometer.enabled: false
        ```
        `feignName` in this example refers to `@FeignClient` value, that is also aliased with `@FeignClient` name and `@FeignClient` `contextId`. In a load-balanced scenario, it also corresponds to the `serviceId` of the server app that will be used to retrieve the instances. The specified classes for decoders, retryer and other ones must have a bean in the Spring context or have a default constructor.
        结合之前对`FeignClientsRegistrar#getClientName`的描述，示例中的`feignName`对应注解`@FeignClient`中依次序`contextId`、`value`、`name`、`serviceId`*（已弃用）*中第一个非空值。
        <br/>
        Default configurations can be specified in the `@EnableFeignClients` attribute `defaultConfiguration` in a similar manner as described above. The difference is that this configuration will apply to *all* Feign clients.
        默认配置可以在注解`@EnableFeignClients`的`defaultConfiguration`属性中配置。
        If you prefer using configuration properties to configure all ·, you can create configuration properties with `default` feign name.
        在配置文件中，可以使用`default`作为feign名字来创建配置。
        示例
        ``` yaml
        spring:
            cloud:
                openfeign:
                    client:
                        config:
                            default:
                                connectTimeout: 5000
                                readTimeout: 5000
                                loggerLevel: basic
        ```
        <br/>
        可配置的项参考：`org.springframework.cloud.openfeign.FeignClientProperties.FeignClientConfiguration`。
        注意：`url`，只有在注解`@FeignClient`中未配置的时候才会被采用。
        ``` Java
        @ConfigurationProperties("spring.cloud.openfeign.client")
        public class FeignClientProperties {
            ...
            /**
             * Feign client configuration.
             */
            public static class FeignClientConfiguration {

                private Logger.Level loggerLevel;

                private Integer connectTimeout;

                private Integer readTimeout;

                private Class<Retryer> retryer;

                private Class<ErrorDecoder> errorDecoder;

                private List<Class<RequestInterceptor>> requestInterceptors;

                private Class<ResponseInterceptor> responseInterceptor;

                private Map<String, Collection<String>> defaultRequestHeaders;

                private Map<String, Collection<String>> defaultQueryParameters;

                private Boolean dismiss404;

                private Class<Decoder> decoder;

                private Class<Encoder> encoder;

                private Class<Contract> contract;

                private ExceptionPropagationPolicy exceptionPropagationPolicy;

                private List<Class<Capability>> capabilities;

                private Class<QueryMapEncoder> queryMapEncoder;

                private MicrometerProperties micrometer;

                private Boolean followRedirects;

                /**
                 * Allows setting Feign client host URL. This value will only be taken into
                 * account if the url is not set in the @FeignClient annotation.
                 */
                private String url;
            ...
        }
        ```

    2. 配置原理
        1. 关键类和成员
            - `org.springframework.boot.context.propertiesFeignAutoConfiguration`
                配置类，通过注解`@EnableConfigurationProperties`引入属性配置`FeignClientProperties`和`FeignHttpClientProperties`。
                ``` Java
                @Configuration(proxyBeanMethods = false)
                @ConditionalOnClass(Feign.class)
                @EnableConfigurationProperties({ FeignClientProperties.class, FeignHttpClientProperties.class,
                        FeignEncoderProperties.class })
                public class FeignAutoConfiguration {
                    ...
                }
                ```
            - `org.springframework.cloud.openfeign.FeignClientProperties`的使用`@ConfigurationProperties`设置外部配置`spring.cloud.openfeign.client`
                ``` Java
                @ConfigurationProperties("spring.cloud.openfeign.client")
                public class FeignClientProperties {
                    ...
                }
                ```
                关键成员：
                - `defaultToProperties`：是否默认属性配置文件优先
                - `defaultConfig`：属性配置文件中的默认配置项
                  默认值：`default`
                  存在这种情况，某个Client的`contextId`的取值就是`default`，这时就和默认的`defaultConfig = "default"`发生冲突。此时，只要修改`defaultConfig`的值即可，并将新的`defaultConfig`值作为默认配置。
                - `config`：各个Client配置的映射：`Map<String, FeignClientConfiguration>`
                  注意：映射`config`中的key的值是`contextId`。参考`org.springframework.cloud.openfeign.FeignClientFactoryBean#configureFeign`。
                - 
            - `org.springframework.cloud.openfeign.FeignClientFactoryBean`
                处理配置关键逻辑
        2.  配置逻辑与覆盖优先级
            [Feign源码阅读（三）FeignClient定制化配置](https://juejin.cn/post/6878568367259975694)
            
            **关键代码：`org.springframework.cloud.openfeign.FeignClientFactoryBean#configureFeign`**
            ``` Java
            public class FeignClientFactoryBean
                    implements FactoryBean<Object>, InitializingBean, ApplicationContextAware, BeanFactoryAware {
            ...

                protected void configureFeign(FeignClientFactory context, Feign.Builder builder) {
                    FeignClientProperties properties = beanFactory != null ? beanFactory.getBean(FeignClientProperties.class)
                            : applicationContext.getBean(FeignClientProperties.class);

                    FeignClientConfigurer feignClientConfigurer = getOptional(context, FeignClientConfigurer.class);
                    setInheritParentContext(feignClientConfigurer.inheritParentConfiguration());

                    if (properties != null && inheritParentContext) {
                        if (properties.isDefaultToProperties()) {
                            configureUsingConfiguration(context, builder);
                            configureUsingProperties(properties.getConfig().get(properties.getDefaultConfig()), builder);
                            configureUsingProperties(properties.getConfig().get(contextId), builder);
                        }
                        else {
                            configureUsingProperties(properties.getConfig().get(properties.getDefaultConfig()), builder);
                            configureUsingProperties(properties.getConfig().get(contextId), builder);
                            configureUsingConfiguration(context, builder);
                        }
                    }
                    else {
                        configureUsingConfiguration(context, builder);
                    }
                }

            ...
            }
            ```
            分析上述代码：
            - `roperties.isDefaultToProperties()`：
                如果`defaultToProperties`为`true`（默认值），则先配置`@FeignClient`配置属性中的`configuration`，再使用属性配置文件中的配置覆盖。符合SpringBoot配置优先的原则。
                反之，`@FeignClient`配置属性中的`configuration`会覆盖属性文件中的配置。
                示例：
                ``` yaml
                spring:
                    cloud:
                        openfeign:
                            client:
                                default-to-properties: true
                ```
            - 属性文件中的默认配置
                `org.springframework.cloud.openfeign.FeignClientProperties#defaultConfig`的默认值采用`"default"`。
                如果某个`contextId`恰好也是`"default"`，这时，只要修改`defaultConfig`的值即可，然后以此值作为key进行默认配置。
                示例：
                ``` yaml
                spring:
                    cloud:
                        openfeign:
                            client:
                                default-config: new-default
                                config:
                                    new-default:
                                        ...
                ```
            - 特定`contextId`的配置
                 如果`contextId`为空，则使用`getName(attributes)`方法从属性中读取`serviceId`、`name`、`value`中第一个非空的值。
                在负载均衡时，`contextId`就是服务id。
                示例：
                ``` yaml
                spring:
                    cloud:
                        openfeign:
                            client:
                                config:
                                    # 如果`contextId`为空，则使用`getName(attributes)`方法从属性中读取`serviceId`、`name`、`value`中第一个非空的值。
                                    # 在负载均衡时，`contextId`就是服务id
                                    my-service:
                                        ...
                ```


5. 通过配置类`@Configuration`来配置
    Creating a bean of one of those type and placing it in a `@FeignClient` configuration (such as FooConfiguration above) allows you to override each one of the beans described. Example:
    ``` Java
    @Configuration
    public class FooConfiguration {
        @Bean
        public Contract feignContract() {
            return new feign.Contract.Default();
        }

        @Bean
        public BasicAuthRequestInterceptor basicAuthRequestInterceptor() {
            return new BasicAuthRequestInterceptor("user", "password");
        }
    }
    ```
6. 

---

#### `OpenFeign`的`Logger`配置

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
            // 这里只是示例，仅供学习用。现在基本都是使用SLF4J来作为log工具，所以在实际应用时，不建议使用。
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
---

### 超时

---

#### 超时处理

- [Timeout Handling](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html#timeout-handling)

1. Timeout Handling
    - `connectTimeout` prevents blocking the caller due to the long server processing time.
      `connectTimeout` 防止因服务器处理时间过长而阻塞呼叫者。
    - `readTimeout` is applied from the time of connection establishment and is triggered when returning the response takes too long.
      `readTimeout` 从连接建立时起应用，并在返回响应时间过长时触发。
    
    > In case the server is not running or available a packet results in *connection refused*. The communication ends either with an error message or in a fallback. This can happen *before* the `connectTimeout` if it is set very low. The time taken to perform a lookup and to receive such a packet causes a significant part of this delay. It is subject to change based on the remote host that involves a DNS lookup.
    > 如果服务器未运行或不可用，数据包会导致*连接被拒绝*。通信以错误消息或回退结束。如果将 `connectTimeout` 设置得很低，则这种情况可能会在 `connectTimeout`*之前*发生。执行查找和接收此类数据包所花费的时间是造成此延迟的很大一部分。它可能会根据涉及 DNS 查找的远程主机而发生变化。
2. 超时默认值
    `defaultToProperties`默认值为`true`，即默认情况下，属性配置文件优先。
    `org.springframework.cloud.openfeign.FeignClientFactoryBean#configureFeign`
    ``` Java
        protected void configureFeign(FeignClientFactory context, Feign.Builder builder) {
            FeignClientProperties properties = beanFactory != null ? beanFactory.getBean(FeignClientProperties.class)
                    : applicationContext.getBean(FeignClientProperties.class);

            FeignClientConfigurer feignClientConfigurer = getOptional(context, FeignClientConfigurer.class);
            setInheritParentContext(feignClientConfigurer.inheritParentConfiguration());

            if (properties != null && inheritParentContext) {
                if (properties.isDefaultToProperties()) {
                    configureUsingConfiguration(context, builder);
                    configureUsingProperties(properties.getConfig().get(properties.getDefaultConfig()), builder);
                    configureUsingProperties(properties.getConfig().get(contextId), builder);
                }
                else {
                    configureUsingProperties(properties.getConfig().get(properties.getDefaultConfig()), builder);
                    configureUsingProperties(properties.getConfig().get(contextId), builder);
                    configureUsingConfiguration(context, builder);
                }
            }
            else {
                configureUsingConfiguration(context, builder);
            }
        }
    ```
    所以，这里也以属性配置文件优先展示描述。否则顺序逆置。
    1. 首先从`FeignClientConfiguration`（`"spring.cloud.openfeign.client"`）中读取
        注意，属性配置文件中先后分为`defaultConfig`（默认`default`）和注解`@FeignClient`中的自定义`contextId`。
        `org.springframework.cloud.openfeign.FeignClientFactoryBean#configureUsingProperties`
        ``` Java
            protected void configureUsingProperties(FeignClientProperties.FeignClientConfiguration config,
                    Feign.Builder builder) {
                ...
                if (!refreshableClient) {
                    connectTimeoutMillis = config.getConnectTimeout() != null ? config.getConnectTimeout()
                            : connectTimeoutMillis;
                    readTimeoutMillis = config.getReadTimeout() != null ? config.getReadTimeout() : readTimeoutMillis;
                    followRedirects = config.isFollowRedirects() != null ? config.isFollowRedirects() : followRedirects;

                    builder.options(new Request.Options(connectTimeoutMillis, TimeUnit.MILLISECONDS, readTimeoutMillis,
                            TimeUnit.MILLISECONDS, followRedirects));
                }
                ...
            }
        ```
        如果未在`FeignClientConfiguration`中配置，则从`Request.Options`配置bean中读取`connectTimeoutMillis`的值。
    2. 从`Request.Options`配置bean中读取`connectTimeoutMillis`的值
        注意，配置bean来自于注解`@FeignClient`中关联自定义`contextId`的`configuration`。
        `org.springframework.cloud.openfeign.FeignClientFactoryBean#configureUsingConfiguration`
        ``` Java
            protected void configureUsingConfiguration(FeignClientFactory context, Feign.Builder builder) {
                ...
                if (options != null) {
                    builder.options(options);
                    readTimeoutMillis = options.readTimeoutMillis();
                    connectTimeoutMillis = options.connectTimeoutMillis();
                    followRedirects = options.isFollowRedirects();
                }
                ...
            }
        ```
    3. 默认值
        如果在属性配置文件和注解`@FeignClient`配置属性中都没有进行配置，则使用默认值。
        默认值是：
        - `readTimeoutMillis`
            默认值`60s`，即 `60 * 1000`。
        - `connectTimeoutMillis`
            默认值是`10s`，即 `10 * 1000`。

        `org.springframework.cloud.openfeign#connectTimeoutMillis`
        ``` Java
        public class FeignClientFactoryBean
        		implements FactoryBean<Object>, InitializingBean, ApplicationContextAware, BeanFactoryAware {
            ...
            private int readTimeoutMillis = new Request.Options().readTimeoutMillis();

            private int connectTimeoutMillis = new Request.Options().connectTimeoutMillis();

            private boolean followRedirects = new Request.Options().isFollowRedirects();
            ...
        }
        ```
        `feign.Request.Options#Options`
        ``` Java
        public final class Request implements Serializable {
          ...
          public static class Options {
            ...
            /**
             * Creates a new Options Instance.
             *
             * @param connectTimeout value.
             * @param connectTimeoutUnit with the TimeUnit for the timeout value.
             * @param readTimeout value.
             * @param readTimeoutUnit with the TimeUnit for the timeout value.
             * @param followRedirects if the request should follow 3xx redirections.
             */
            public Options(long connectTimeout, TimeUnit connectTimeoutUnit,
                long readTimeout, TimeUnit readTimeoutUnit,
                boolean followRedirects) {
              super();
              this.connectTimeout = connectTimeout;
              this.connectTimeoutUnit = connectTimeoutUnit;
              this.readTimeout = readTimeout;
              this.readTimeoutUnit = readTimeoutUnit;
              this.followRedirects = followRedirects;
              this.threadToMethodOptions = new ConcurrentHashMap<>();
            }
            ...
            /**
             * Creates the new Options instance using the following defaults:
             * <ul>
             * <li>Connect Timeout: 10 seconds</li>
             * <li>Read Timeout: 60 seconds</li>
             * <li>Follow all 3xx redirects</li>
             * </ul>
             */
            public Options() {
              this(10, TimeUnit.SECONDS, 60, TimeUnit.SECONDS, true);
            }
            ...
          }
          ...
        }
        ```
3. 几个注意事项
    - 不推荐对影响数据变更的接口采取重试操作。重试尝试只用于“读”操作。

---

#### 超时重试

1. 默认`Entryer`
   A bean of `Retryer.NEVER_RETRY` with the type `Retryer` is created by default, which will disable retrying. Notice this retrying behavior is different from the Feign default one, where it will automatically retry IOExceptions, treating them as transient network related exceptions, and any RetryableException thrown from an ErrorDecoder.
   默认情况下会创建一个类型为 `Retryer` 的 `Retryer.NEVER_RETRY` bean，这将禁用重试。请注意，此重试行为与 Feign 默认行为不同，在 Feign 默认行为中，它会自动重试 IOException，将其视为瞬时网络相关异常，以及从 ErrorDecoder 抛出的任何 RetryableException。

2. 重试原理
    `feign.SynchronousMethodHandler#invoke`
    执行步骤：
    - 如果当前请求失败，即，报异常，则使用`Retryer`进行重试判断和延时操作
    - `Retryer`延时后，继续执行上一步；如此循环

    ``` Java
    final class SynchronousMethodHandler implements MethodHandler {
        ...
      @Override
      public Object invoke(Object[] argv) throws Throwable {
        RequestTemplate template = buildTemplateFromArgs.create(argv);
        Options options = findOptions(argv);
        Retryer retryer = this.retryer.clone();
        while (true) {
          try {
            return executeAndDecode(template, options);
          } catch (RetryableException e) {
            try {
              retryer.continueOrPropagate(e);
            } catch (RetryableException th) {
              Throwable cause = th.getCause();
              if (propagationPolicy == UNWRAP && cause != null) {
                throw cause;
              } else {
                throw th;
              }
            }
            if (logLevel != Logger.Level.NONE) {
              logger.logRetry(metadata.configKey(), logLevel);
            }
            continue;
          }
        }
      }
        ...
    }
    ```

3. 使用默认构造方法进行属性文件配置
    ``` Java
    spring:
        cloud:
            openfeign:
                client:
                    config:
                        feignName:
                            # retryer: feign.Retryer.Default
                            retryer: com.example.SimpleRetryer
    ```
4. 自定义构造方法
    示例：
    ``` Java
    @FeignClient(name = "foo-product", contextId = "foo-product-api", path="/api", configuration = FooFeignClient4.Configuration.class)
    public interface FooFeignClient4 {

        class Configuration {
            // @Bean
            // public Retryer retryer() {
            //     return new Retryer.Default(3000, 10, 4);
            // }

            @Bean
            public Retryer retryer() {
                return new MyRetryer();
            }

            @Slf4j
            static class MyRetryer implements Retryer {
                long interval;
                int index = 1;
                int count;

                public MyRetryer() {
                    this.interval = 1000 * 2;
                    this.count = 6;
                }

                public MyRetryer(long interval, int count) {
                    this.interval = interval;
                    this.index = count;
                }

                @Override
                public void continueOrPropagate(RetryableException e) {
                    if (index >= count) {
                        log.info("try index = {}", index, e);
                        throw e;
                    }
                    log.info("try index = {}", index);
                    index++;
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                public Retryer clone() {
                    Retryer retryer;
                    try {
                        retryer = (Retryer) super.clone();
                    } catch (CloneNotSupportedException e) {
                        throw new RuntimeException(e);
                    }
                    return retryer;
                }
            }
        }
        
        @GetMapping("/sayHello")
        String sayHi(@RequestParam("name") String name);

    }
    ```

---

### 拦截器 interceptor

1. 代码逻辑
    `org.springframework.cloud.openfeign.FeignClientFactoryBean#configureUsingConfiguration`
    ``` Java
        protected void configureUsingConfiguration(FeignClientFactory context, Feign.Builder builder) {
            ...
            Map<String, RequestInterceptor> requestInterceptors = getInheritedAwareInstances(context,
                    RequestInterceptor.class);
            if (requestInterceptors != null) {
                List<RequestInterceptor> interceptors = new ArrayList<>(requestInterceptors.values());
                AnnotationAwareOrderComparator.sort(interceptors);
                builder.requestInterceptors(interceptors);
            }
            ...
        }
    ```

2. 属性文件配置（yaml）
    实例：
    ``` yaml
    spring:
        cloud:
            openfeign:
                client:
                    config:
                        feignName:
                            requestInterceptors:
                                - com.example.FooRequestInterceptor
                                - com.example.BarRequestInterceptor
                            responseInterceptor: com.example.BazResponseInterceptor
    ```

3. 通过配置属性配置
    1. 注意事项（实验勘疑）
       1. 请求拦截器：`requestInterceptors`
           注意，尽管文档中描述`OpenFeign`会扫描`Collection<RequestInterceptor>`，但是实际验证发现，尽管会扫描到`Collection<RequestInterceptor>`的bean，但是`OpenFeign`并没有使用该数据。
           > Spring Cloud OpenFeign does not provide the following beans by default for feign, but still looks up beans of these types from the application context to create the Feign client:
           > - Collection<RequestInterceptor>
           如果想为某`contextId`配置拦请求截器，必须单独配置多个bean才行。
       2. 相应拦截器：`responseInterceptor`
           `feign.ResponseInterceptor#intercept`方法，建议以如下代码作为返回值：
           ``` Java
           return chain.next(invocationContext);
           ```
    2. 示例
        ``` Java
        @FeignClient(name = "service-foo", contextId = "service-foo-auth", path = "/auth", configuration = FooFeignClient.FeignClientConfiguration.class)
        public interface FooFeignClient {
        
            class FeignClientConfiguration {
        
                // FeignClientFactoryBean#configureUsingConfiguration并不会直接使用Collection<RequestInterceptor>形式的bean注册
                // 需要在中注册多个单独的RequestInterceptor类型的bean来实现。
        
                // @Bean
                // public Collection<RequestInterceptor> requestInterceptors() {
                //     Collection<RequestInterceptor> requestInterceptors = new ArrayList<RequestInterceptor>();
                //     requestInterceptors.add(new MyRequestInterceptor());
                //     return requestInterceptors;
                // }
        
                // 注册RequestInterceptor实例01
                @Bean
                public RequestInterceptor requestInterceptor1() {
                    return new MyRequestInterceptor();
                }
        
                // 注册RequestInterceptor实例02
                @Bean
                public RequestInterceptor requestInterceptor2() {
                    return new MyRequestInterceptor();
                }
        
                @Bean
                public ResponseInterceptor responseInterceptor() {
                    return new MyResponseInterceptor();
                }
            }
        
            // 自定义请求拦截器
            @Slf4j
            class MyRequestInterceptor implements RequestInterceptor {
        
                // 添加一个实例跟踪变量，验证Collection<RequestInterceptor>的注册方式。
                private static int instanceCount;
                private int instanceIndex;
        
                public MyRequestInterceptor() {
                    instanceCount++;
                    instanceIndex = instanceCount;
                    log.info("constructor. instance index = {}", instanceIndex);
                }
        
                private static final String[] AVAILABLE_TOKENS =
                        {
                                // available
                                "hello",
                                "world",
                                // unavailable
                                "ZhangSan",
                                "Lisi",
                                "WangWu",
                                "ZhaoLiu",
                        };
        
                @Override
                public void apply(RequestTemplate template) {
                    int index = (int) (Math.random() * AVAILABLE_TOKENS.length);
                    String account = AVAILABLE_TOKENS[index];
        
                    // 添加header：Authorization
                    String encoded1 = Base64.getEncoder().encodeToString(("Basic " + account).getBytes(StandardCharsets.UTF_8));
                    template.removeHeader("Authorization"); // 先清空，否则自定义会叠加。
                    template.header("Authorization", encoded1);
                    // 添加header：X-token
                    String encoded2 = Base64.getEncoder().encodeToString(account.getBytes(StandardCharsets.UTF_8));
                    template.removeHeader("X-token"); // 先清空，否则自定义会叠加。
                    template.header("X-token", encoded2);
        
                    // 下面的header不会发生叠加
                    template.header("Accept", "application/json");
                    template.header("Content-Type", "application/json");
                    template.header("Accept-Encoding", "gzip, deflate");
                    template.header("Accept-Language", "en-US,en;q=0.8");
                    template.header("Cache-Control", "no-cache");
        
                    log.info("instance index = {}. account = {}, Authorization = {}, X-token = {}, template = {}", instanceIndex, account, encoded1, encoded2, template);
                }
            }
        
            // 自定义相应拦截器
            @Slf4j
            class MyResponseInterceptor implements ResponseInterceptor {
        
                @Override
                public Object intercept(InvocationContext invocationContext, Chain chain) throws Exception {
                    Response response = invocationContext.response();
                    String data = response.body().toString();
                    return chain.next(invocationContext);
                }
            }
        
            @PostMapping("queryWithAuthorization")
            String queryWithAuthorization(@RequestParam("name") String name);
        
            @PostMapping("queryWithXToken")
            String queryWithXToken(@RequestParam("name") String name);
        
        }
        ```
        示例中的header使用来源：
        [请求头 x-token Authorization](https://www.cnblogs.com/zychuan/p/17811401.html)
        ``` txt
        X-Token
        X-Token是一种自定义的身份验证方式，通常用于API接口的身份验证。在使用X-Token进行身份验证时，客户端需要在请求头中添加一个名为X-Token的字段，字段的值为用户的身份令牌。服务器接收到请求后，会验证该令牌的有效性，如果有效则认为身份验证通过。

        Authorization
        Authorization是一种标准的身份验证方式，通常用于Web应用程序的身份验证。在使用Authorization进行身份验证时，客户端需要在请求头中添加一个名为Authorization的字段，字段的值为一个包含身份验证信息的字符串，通常是基于Base64编码的用户名和密码组合。服务器接收到请求后，会解码该字符串并验证用户名和密码的有效性，如果有效则认为身份验证通过。

        总结来说，X-Token是一种自定义的身份验证方式，而Authorization是一种标准的身份验证方式。
        ```
4. 



---

#### 请求拦截器 `Collection<RequestInterceptor>`

1. 

---

#### 响应拦截器
