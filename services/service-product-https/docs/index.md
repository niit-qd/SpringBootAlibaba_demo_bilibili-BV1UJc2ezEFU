[TOC]

---

### `HTTPS`服务端：创建`keystore`

参考：

- [SSL3、TLS1.0、TSL1.1、TLS1.2、TLS 1.3协议版本间的差异](https://blog.csdn.net/WoTrusCA/article/details/109839209)
- [SpringBoot配置Https](https://www.cnblogs.com/wdadwa/articles/18321338)
- [Spring Boot 配置 TLS](https://springdoc.cn/spring-tls-setup)
- [jdk中如何设置tls的版本](https://docs.pingcode.com/baike/3186213)
- [如何解决由于 TLS 版本不兼容导致的问题](https://xie.infoq.cn/article/a444df132485d70510a2dae88)

1. 生成本地证书
    ```shell
    keytool -genkey -alias tomcat -storetype PKCS12 -keyalg RSA -keysize 2048 -keystore keystore.p12 -validity 3650
    ```
2. 配置
    ```yaml
    server:
      ssl:
        enabled: true
        key-store: classpath:keystore.p12 #keystore 可以替换成自己证书的名字
        key-store-password: 123456        #创建证书时填写的密码
        keyStoreType: PKCS12 
        keyAlias: tomcat
        protocol: TLS
    ```

   **几个要的问题**
    - `server.ssl.client-auth`
      `server.ssl.client-auth=need`：客户端无法访问https服务，返回：
      ``` text
      此网站无法提供安全连接localhost 不接受您的登录证书，或者您可能没有提供登录证书。
      请尝试联系系统管理员。
      ERR_BAD_SSL_CLIENT_AUTH_CERT
      ```
    - `enabled-protocols`
      以`tomcat-embed-core-10.1.24-sources.jar!\org\apache\tomcat\util\net\SSLHostConfig.java`作为参考，查询到可支持的协议
      `SSL_PROTO_ALL_SET`如下：
        - TLSv1.3
        - TLSv1.2
        - TLSv1.1
        - TLSv1
        - SSLv3
        - SSLv2Hello

      具体逻辑：
      `tomcat-embed-core-10.1.24-sources.jar`
      1. Tomcat中SSL注册的协议
          构造方法：`org.apache.tomcat.util.net.SSLUtilBase#SSLUtilBase`
          ``` Java
                  Set<String> configuredProtocols = sslHostConfig.getProtocols();
          ```
          `configuredProtocols`的取值过程是，如果给定了配置，则使用配置，否则默认所有协议`SSL_PROTO_ALL_SET`。          
          `org.apache.tomcat.util.net.SSLHostConfig#setProtocols`
          ``` Java
              public void setProtocols(String input) {
                  protocols.clear();
                  explicitlyRequestedProtocols.clear();

                  // List of protocol names, separated by ",", "+" or "-".
                  // Semantics is adding ("+") or removing ("-") from left
                  // to right, starting with an empty protocol set.
                  // Tokens are individual protocol names or "all" for a
                  // default set of supported protocols.
                  // Separator "," is only kept for compatibility and has the
                  // same semantics as "+", except that it warns about a potentially
                  // missing "+" or "-".

                  // Split using a positive lookahead to keep the separator in
                  // the capture so we can check which case it is.
                  for (String value: input.split("(?=[-+,])")) {
                      String trimmed = value.trim();
                      // Ignore token which only consists of prefix character
                      if (trimmed.length() > 1) {
                          if (trimmed.charAt(0) == '+') {
                              trimmed = trimmed.substring(1).trim();
                              if (trimmed.equalsIgnoreCase(Constants.SSL_PROTO_ALL)) {
                                  protocols.addAll(SSL_PROTO_ALL_SET);
                              } else {
                                  protocols.add(trimmed);
                                  explicitlyRequestedProtocols.add(trimmed);
                              }
                          } else if (trimmed.charAt(0) == '-') {
                              trimmed = trimmed.substring(1).trim();
                              if (trimmed.equalsIgnoreCase(Constants.SSL_PROTO_ALL)) {
                                  protocols.removeAll(SSL_PROTO_ALL_SET);
                              } else {
                                  protocols.remove(trimmed);
                                  explicitlyRequestedProtocols.remove(trimmed);
                              }
                          } else {
                              if (trimmed.charAt(0) == ',') {
                                  trimmed = trimmed.substring(1).trim();
                              }
                              if (!protocols.isEmpty()) {
                                  log.warn(sm.getString("sslHostConfig.prefix_missing",
                                          trimmed, getHostName()));
                              }
                              if (trimmed.equalsIgnoreCase(Constants.SSL_PROTO_ALL)) {
                                  protocols.addAll(SSL_PROTO_ALL_SET);
                              } else {
                                  protocols.add(trimmed);
                                  explicitlyRequestedProtocols.add(trimmed);
                              }
                          }
                      }
                  }
              }
          ```
          
          取值过程是：
          - 对配置值`input`使用`"(?=[-+,])"`执行`split`。注意，该正则表达式会带上分隔符。
          - 遍历每一个分段
            - 以`+`开头，对后面的内容
              - `all`: 为`protocols`添加列表`SSL_PROTO_ALL_SET`中指定的所有协议
              - 其它：为协议列表`protocols`添加该值；并且为明确请求的协议列表`explicitlyRequestedProtocols`添加相同值。
            - 以`-`开头，对后面的内容
              - `all`: 为`protocols`删除列表`SSL_PROTO_ALL_SET`中指定的所有协议
              - 其它：为协议列表`protocols`删除该值；并且为明确请求的协议列表`explicitlyRequestedProtocols`删除相同值。
            - 以`，`开头，对后面的内容
              逻辑同`+`
            - 其它情形，即不以`+`、`-`、`，`开头
              直接作为值，参考`+`逻辑
          
          上述描述只是代码逻辑角度上的描述，换一个描述，如下：
          - `input`取值可以使用`+{protocol}`、`-{protocol}`、`,{protocol}`形式的任何分段组合。
            例如，`+all-TLSv1.2-TLSv1.3,TLSv1.2`
          - 使用多个分段组合时，根据顺序，后面的分段会影响前面已经配置的值。
            例如，`+all-TLSv1.2-TLSv1.3,TLSv1.2`，会先并入所有的协议，然后删除`TLSv1.2`，然后删除`TLSv1.3`，然后再添加`TLSv1.2`。
          - 可以不带分隔符
            例如，`all`添加所有协议，`TLSv1.3`只支持`TLSv1.3`这一个协议。
          
          验证示例：使用`-all`就相当于减掉所有协议，等效于配置为空字符串。
          
          `setProtocols(String input)`方法的调用，会根据配置值有不同的处理：
          - 处理流程：
            `spring-boot-3.2.6-sources.jar!\org\springframework\boot\web\embedded\tomcat\SslConnectorCustomizer.java`
            `org.springframework.boot.web.embedded.tomcat`流程顺序
            - `update(SslBundle updatedSslBundle)`
            - `customize(updatedSslBundle);`
            - `configureSsl(sslBundle, (AbstractHttp11JsseProtocol<?>) handler);`
              - 构造方法
                `SSLHostConfig sslHostConfig = new SSLHostConfig();`
                ``` Java
                    public SSLHostConfig() {
                        // Set defaults that can't be (easily) set when defining the fields.
                        setProtocols(Constants.SSL_PROTO_ALL);
                    }
                ```
                构造方法默认使用`input`值`all`。所以，默认添加所有协议`SSL_PROTO_ALL_SET`。
              - 继续
                - `applySslBundle(sslBundle, protocol, sslHostConfig);`
                - `configureEnabledProtocols(sslHostConfig, options);`
                  ``` Java
                    private void configureEnabledProtocols(SSLHostConfig sslHostConfig, SslOptions options) {
                      if (options.getEnabledProtocols() != null) {
                        String enabledProtocols = StringUtils.arrayToDelimitedString(options.getEnabledProtocols(), "+");
                        sslHostConfig.setProtocols(enabledProtocols);
                      }
                    }
                  ```
                  如果在SpringBoot中配置的已启用的参数不为空，则以该值作为`input`的值，再执行一次`setProtocols`。
                  注意，每次执行`setProtocols`，都会先清空之前的协议列表。所以，指定`input`值后，会对协议进行重新设置。
                  **特别注意**：如果`input`的值是空字符串，例如`""`、`"  "`，根据上面的逻辑，会导致协议列表为空，SpringBoot会报`None of the [protocols] specified are supported by the SSL engine`错误。从上面的逻辑看就知道，这实际上是SpringBoot代码上的一个逻辑缺陷，只要把排除项加上判空就没有问题了。
                  ``` log
                  Caused by: java.lang.IllegalArgumentException: None of the [protocols] specified are supported by the SSL engine : [[]]
                    at org.apache.tomcat.util.net.SSLUtilBase.getEnabled(SSLUtilBase.java:164) ~[tomcat-embed-core-10.1.24.jar:10.1.24]
                    at org.apache.tomcat.util.net.SSLUtilBase.<init>(SSLUtilBase.java:112) ~[tomcat-embed-core-10.1.24.jar:10.1.24]
                    at org.apache.tomcat.util.net.jsse.JSSEUtil.<init>(JSSEUtil.java:61) ~[tomcat-embed-core-10.1.24.jar:10.1.24]
                    at org.apache.tomcat.util.net.jsse.JSSEUtil.<init>(JSSEUtil.java:56) ~[tomcat-embed-core-10.1.24.jar:10.1.24]
                    at org.apache.tomcat.util.net.jsse.JSSEImplementation.getSSLUtil(JSSEImplementation.java:52) ~[tomcat-embed-core-10.1.24.jar:10.1.24]
                    at org.apache.tomcat.util.net.AbstractJsseEndpoint.createSSLContext(AbstractJsseEndpoint.java:95) ~[tomcat-embed-core-10.1.24.jar:10.1.24]
                    at org.apache.tomcat.util.net.AbstractJsseEndpoint.initialiseSsl(AbstractJsseEndpoint.java:70) ~[tomcat-embed-core-10.1.24.jar:10.1.24]
                    at org.apache.tomcat.util.net.NioEndpoint.bind(NioEndpoint.java:199) ~[tomcat-embed-core-10.1.24.jar:10.1.24]
                    at org.apache.tomcat.util.net.AbstractEndpoint.bindWithCleanup(AbstractEndpoint.java:1286) ~[tomcat-embed-core-10.1.24.jar:10.1.24]
                    at org.apache.tomcat.util.net.AbstractEndpoint.start(AbstractEndpoint.java:1372) ~[tomcat-embed-core-10.1.24.jar:10.1.24]
                    at org.apache.coyote.AbstractProtocol.start(AbstractProtocol.java:635) ~[tomcat-embed-core-10.1.24.jar:10.1.24]
                    at org.apache.catalina.connector.Connector.startInternal(Connector.java:1044) ~[tomcat-embed-core-10.1.24.jar:10.1.24]
                    ... 19 common frames omitted
                  ```
          
          在SpringBoot中，`input`的配置参数是：**`server.ssl.enabled-protocols`**。
          该参数的配置流程如下：
          - `org.springframework.boot.web.server.WebServerSslBundle`: 构造方法`WebServerSslBundle(SslStoreBundle stores, String keyPassword, Ssl ssl)`
            获取参数`options`.
            ``` Java
              private WebServerSslBundle(SslStoreBundle stores, String keyPassword, Ssl ssl) {
                this.stores = stores;
                this.key = SslBundleKey.of(keyPassword, ssl.getKeyAlias());
                this.protocol = ssl.getProtocol();
                this.options = SslOptions.of(ssl.getCiphers(), ssl.getEnabledProtocols());
                this.managers = SslManagerBundle.from(this.stores, this.key);
              }
            ```
          - `spring-boot-3.2.6-sources.jar!\org\springframework\boot\web\embedded\tomcat\TomcatServletWebServerFactory.java`
            ``` Java
              private void customizeSsl(Connector connector) {
                SslConnectorCustomizer customizer = new SslConnectorCustomizer(logger, connector, getSsl().getClientAuth());
                customizer.customize(getSslBundle());
                String sslBundleName = getSsl().getBundle();
                if (StringUtils.hasText(sslBundleName)) {
                  getSslBundles().addBundleUpdateHandler(sslBundleName, customizer::update);
                }
              }
            ```
            调用了`SslConnectorCustomizer#update`方法
            

          `SSL_PROTO_ALL_SET`的取值过程如下：
          `org.apache.tomcat.util.net.SSLHostConfig`
          ``` Java
              protected static final Set<String> SSL_PROTO_ALL_SET = new HashSet<>();
              static {
                  /* Default used if protocols are not configured, also used if
                  * protocols="All"
                  */
                  SSL_PROTO_ALL_SET.add(Constants.SSL_PROTO_SSLv2Hello);
                  SSL_PROTO_ALL_SET.add(Constants.SSL_PROTO_TLSv1);
                  SSL_PROTO_ALL_SET.add(Constants.SSL_PROTO_TLSv1_1);
                  SSL_PROTO_ALL_SET.add(Constants.SSL_PROTO_TLSv1_2);
                  SSL_PROTO_ALL_SET.add(Constants.SSL_PROTO_TLSv1_3);
              }
          ```
          `public void setProtocols(String input)`
          SSL注册的协议：
          ``` Java
          protocols.addAll(SSL_PROTO_ALL_SET);
          ```
          返回值：
          ``` Java
              public Set<String> getProtocols() {
                  return protocols;
              }
          ```
          排除项：
          - `TLSv1.3`，如果它没有被实现或者没有明确指定，会被排除。
            ``` Java
                    // If TLSv1.3 is not implemented and not explicitly requested we can
                    // ignore it. It is included in the defaults so it may be configured.
                    if (!implementedProtocols.contains(Constants.SSL_PROTO_TLSv1_3) &&
                            !sslHostConfig.isExplicitlyRequestedProtocol(Constants.SSL_PROTO_TLSv1_3)) {
                        configuredProtocols.remove(Constants.SSL_PROTO_TLSv1_3);
                    }
            ```
          - 新的JRE已经不再支持`SSLv2Hello`。如果它没有被实现或者没有明确指定，会被排除。
            ``` Java
                    // Newer JREs are dropping support for SSLv2Hello. If it is not
                    // implemented and not explicitly requested we can ignore it. It is
                    // included in the defaults so it may be configured.
                    if (!implementedProtocols.contains(Constants.SSL_PROTO_SSLv2Hello) &&
                            !sslHostConfig.isExplicitlyRequestedProtocol(Constants.SSL_PROTO_SSLv2Hello)) {
                        configuredProtocols.remove(Constants.SSL_PROTO_SSLv2Hello);
                    }
            ```
      2. Tomcat中已实现的协议
          ``` Java
                  Set<String> implementedProtocols = getImplementedProtocols();
          ```
          `getImplementedProtocols`方法来自于实现类`org.apache.tomcat.util.net.jsse.JSSEUtil`。
          ``` Java
              protected Set<String> getImplementedProtocols() {
                  initialise();
                  return implementedProtocols;
              }
              private void initialise() {
                  if (!initialized) {
                      synchronized (this) {
                          if (!initialized) {
          ...
                              String[] implementedProtocolsArray = context.getSupportedSSLParameters().getProtocols();
                              implementedProtocols = new HashSet<>(implementedProtocolsArray.length);
                              for (String protocol : implementedProtocolsArray) {
                                  String protocolUpper = protocol.toUpperCase(Locale.ENGLISH);
                                  if (!"SSLV2HELLO".equals(protocolUpper) && !"SSLV3".equals(protocolUpper)) {
                                      if (protocolUpper.contains("SSL")) {
                                          log.debug(sm.getString("jsseUtil.excludeProtocol", protocol));
                                          continue;
                                      }
                                  }
                                  implementedProtocols.add(protocol);
                              }
                          }
                      }
                  }
              }
          ...

          ```
          上面代码通过`getSupportedSSLParameters().getProtocols()`先查找JDK中已支持的协议。
          JDK中支持的协议可以参考：
          `openjdk-23.0.2\lib\src.zip!\java.base\sun\security\ssl\SSLContextImpl.java#AbstractTLSContext`支持的协议`supportedProtocols`是：
          ``` Java
                  private static final List<CipherSuite> supportedCipherSuites;
                  private static final List<CipherSuite> serverDefaultCipherSuites;

                  static {
                      supportedProtocols = Arrays.asList(
                          ProtocolVersion.TLS13,
                          ProtocolVersion.TLS12,
                          ProtocolVersion.TLS11,
                          ProtocolVersion.TLS10,
                          ProtocolVersion.SSL30,
                          ProtocolVersion.SSL20Hello
                      );

                      serverDefaultProtocols = getAvailableProtocols(
                              new ProtocolVersion[] {
                          ProtocolVersion.TLS13,
                          ProtocolVersion.TLS12,
                          ProtocolVersion.TLS11,
                          ProtocolVersion.TLS10
                      });

                      supportedCipherSuites = getApplicableSupportedCipherSuites(
                              supportedProtocols);
                      serverDefaultCipherSuites = getApplicableEnabledCipherSuites(
                              serverDefaultProtocols, false);
                  }
          ```
          `openjdk-23.0.2\lib\src.zip!\java.base\sun\security\ssl\ProtocolVersion.java#ProtocolVersion`
          ``` Java
          enum ProtocolVersion {
              TLS13           (0x0304,    "TLSv1.3",      false),
              TLS12           (0x0303,    "TLSv1.2",      false),
              TLS11           (0x0302,    "TLSv1.1",      false),
              TLS10           (0x0301,    "TLSv1",        false),
              SSL30           (0x0300,    "SSLv3",        false),
              SSL20Hello      (0x0002,    "SSLv2Hello",   false),

              DTLS12          (0xFEFD,    "DTLSv1.2",     true),
              DTLS10          (0xFEFF,    "DTLSv1.0",     true),

              final int id;
              final String name;
              final boolean isDTLS;
              final byte major;
              final byte minor;
              final boolean isAvailable;

              ProtocolVersion(int id, String name, boolean isDTLS) {...}
          }
          ```
        
        
        3. 筛选`enabledProtocols`
            如果`implementedProtocols`为空，则取所有的`configuredProtocols`。否则，取`configuredProtocols`和`implementedProtocols`的交集。
            ``` Java
                    List<String> enabledProtocols =
                            getEnabled("protocols", getLog(), warnTls13, configuredProtocols, implementedProtocols);
            ```

3. 效果
    通过浏览器访问 `http://localhost:8015/product/get/12` ，返回：
    ``` text
    Bad Request
    This combination of host and port requires TLS.
    ```
    通过浏览器访问 `https://localhost:8015/product/get/12` ，返回：
    ```text
    
    ```


---

### `HTTPS`客户端：导入证书

【参考】
- [【Java可执行命令】（十一）Java 密钥库和证书管理工具keytool：玩转密钥库和证书管理，深入解析keytool工具的应用与技巧~](https://blog.csdn.net/LVSONGTAO1225/article/details/131518523)
- [Java Keytool生成数字证书(*.cer/*.p12)文件](https://www.cnblogs.com/huizhipeng/p/16379606.html)
- [Keytool工具的基本使用](https://www.cnblogs.com/werr370/p/16371906.html)
- [使用JDK自带的工具keytool生成证书](https://blog.csdn.net/daqiang012/article/details/86713577)
- [Java密钥库及keytool使用详解](https://blog.csdn.net/a82514921/article/details/104588573/)
- [在 QRadar 上设置基于证书的认证](https://www.ibm.com/docs/zh/qradar-common?topic=qradar-setting-up-certificate-based-authentication)
- [Delete a certificate from a keystore with keytool](https://www.tbs-certificates.co.uk/FAQ/en/supprimer-certificat-keystore-java.html)
- [Self-signed certificate: DNSName components must begin with a letter](https://stackoverflow.com/questions/33827789/self-signed-certificate-dnsname-components-must-begin-with-a-letter)


1. 问题：`unable to find valid certification path to requested target`
    - `openfeign`使用`url`直接访问
      ``` Java
      @FeignClient(name = "service-product-https-directly", url = "https://localhost:8015", path = "/product")
      ```
      ``` log
      feign.RetryableException: (certificate_unknown) PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target executing GET https://localhost:8015/product/get/100
      ```
    - `openfeign`使用`name`通过负载均衡访问
      ``` Java
      @FeignClient(name = "service-product-https", path = "/product")
      ```
      ``` log
      feign.RetryableException: (certificate_unknown) PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target executing GET https://service-product-https/product/get/100
      ```
    
    需要基于当前`keystore`导出`cert`证书，并将证书导入到当前`JRE`环境的`cacerts`。
    ``` shell
    # 导出cer证书
    keytool -exportcert -keystore  .\keystore.p12 -alias tomcat -file certfile.cer
    # 将证书导入到JRE
    keytool -import -alias tomcat -file certfile.cer -keystore {JAVA_HOME}/lib/security/cacerts
    ```

2. 问题：`(certificate_unknown) No name matching localhost found`
    - `openfeign`使用`url`直接访问
      ``` log
      feign.RetryableException: (certificate_unknown) No name matching localhost found executing GET https://localhost:8015/product/get/100
      ```
    - `openfeign`使用`name`通过负载均衡访问
      ``` log
      feign.RetryableException: (certificate_unknown) No name matching localhost found executing GET https://service-product-https/product/get/100
      ```
    
    创新创建`keystore`，指定`ext`中的`DNS`或者`IP`。
    ``` shell
    # 重新创建keystore的时候指定dns或者ip。如果希望适配多个dns或者ip，使用逗号","隔开即可。
    rm keystore.p12
    keytool -genkey -alias tomcat -storetype PKCS12 -keyalg RSA -keysize 2048 -keystore keystore.p12 -validity 3650 -ext san=dns:localhost,dns:otherhost,ip:127.0.0.1,ip:192.168.185.1
    # 如果JRE中已经存在该证书，先删除
    keytool.exe -delete -alias tomcat -keystore {JAVA_HOME}/lib/security/cacerts
    # 将证书导入到JRE
    keytool -import -alias tomcat -file certfile.cer -keystore {JAVA_HOME}/lib/security/cacerts
    ```

3. 问题：负载均衡`Bad Request This combination of host and port requires TLS.`
    ``` Java
    @FeignClient(name = "service-product-https", path = "/product")
    ```
    ``` log
    feign.FeignException$BadRequest: [400] during [GET] to [http://service-product-https/product/get/100] [ProductHttpsFeignClient2#getProduct(Long)]: [Bad Request
    This combination of host and port requires TLS.
    ]
    ```

    1. 解决方案
        需要让服务端注册的时候将自己注册为`https`服务。
        将`spring.cloud.nacos.discovery.secure`配置为`true`。
        ``` yaml
        spring:
          application:
            name: service-product-https
          cloud:
            nacos:
              discovery:
                secure: true
              server-addr: 127.0.0.1:8848
        ```
    2. 原理
      - 配置
         `spring.cloud.nacos.discovery.secure`：当前服务是否是一个`https`服务
         `spring-cloud-starter-alibaba-nacos-discovery-2023.0.1.0-sources.jar!\com\alibaba\cloud\nacos\NacosDiscoveryProperties.java`
          ``` Java
            /**
             * whether your service is a https service.
            */
            private boolean secure = false;
          ```
      - Java代码
        根据`secure`属性判断是否是`https`来拼接`uri`。
        `spring-cloud-commons-4.1.0-sources.jar!\org\springframework\cloud\client\DefaultServiceInstance.java`
        ``` Java
          public static URI getUri(ServiceInstance instance) {
            String scheme = (instance.isSecure()) ? "https" : "http";
            int port = instance.getPort();
            if (port <= 0) {
              port = (instance.isSecure()) ? 443 : 80;
            }
            String uri = String.format("%s://%s:%s", scheme, instance.getHost(), port);
            return URI.create(uri);
          }
        ```

    **根据如上分析，可以假设存在这么一种情况，有多个同名服务，但是`spring.cloud.nacos.discovery.secure`属性有的是`false`，有的是`true`，即有的是`http`服务，有的服务是`https`服务。这种情况下，两种`schema`同时存在的情况下，这两种服务是可以共存的。**
    无论在注解`FeignClient`中是否为`name`属性添加`https://`前缀，都会通过负载均衡正确访问服务。建议不追加`https://`前缀。
