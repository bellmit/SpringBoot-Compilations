# 传统的ELK的部署与使用

单体部署我们的Spring Boot项目的时候，日志通常都是放在服务器目录，当我们部署我们的分布式或者集群项目之后，这种方式已经是行不通了，我们需要搭建一套日志系统进行统一日志处理。

ELK (Elasticsearch, Logstash, Kibana)是一个传统的比较成熟的方案。下面我们就是用Docker部署并进行日志的收集处理。

<a name="Oje7J"></a>
# 安装配置
1、拉取项目代码

```java
git clone https://github.com/deviantony/docker-elk.git
```

2、进入到docker-elk目录进行创建启动容器<br />修改该目录下的** .env** 文件中的配置可以修改我们所需要使用的ES的版本。
```
docker-compose up -d
```

3、启动后，进入Kibana的控制台：http://ip:5601

- 默认登录的账号密码为：

账号：elastic<br />密码：changeme

- 登录进入后可以进行修改密码，此处修改的密码是修改ES、Kibana以及logstash使用的密码。
- 这个安全认证的功能是安装了x-pack的安全认证的插件，x-pack的功能是收费的，试用期30天。可以自行选择是否关闭。

[https://www.elastic.co/guide/en/elastic-stack-overview/7.3/license-expiration.html#_graph](https://www.elastic.co/guide/en/elastic-stack-overview/7.3/license-expiration.html#_graph)

- 进入elasticsearch.yml的配置文件中进行关闭该功能：xpack.security.enabled: false

4、修改下logstash的配置

```
input{
        tcp {
                mode => "server"
                port => 5000
                codec => json_lines
                tags => ["data-http"]
        }
}
filter{
    json{
        source => "message"
        remove_field => ["message"]
    }
}
output{
    if "data-http" in [tags]{
        elasticsearch{
                hosts=> ["192.168.233.137:9200"]
                index => "data-http-%{+YYYY.MM.dd}"
                user => "elastic"
		            password => "changeme"
                }
        stdout{codec => rubydebug}
    }
}
```

- filter将message字段去掉，只是为了当展示springboot的http请求接口的数据更加规整，而不是全部展示在message字段中。
- output标签将数据传递给了elasticsearch，这里使用了if，当判断所出数据为所指定tag，才进行下面的配置。特别要注意index的配置，该值在kibana中需要使用，这里指定的index值为：data-http，要注意该值与tag是没有关系的，要注意区分。

5、安装一个插件测试一下：

```
yum -y install nc
```
发送一个TCP的请求测试：

```
echo "test" | nc 192.168.233.137 5000
```
6、可以进入Kibana控制台查看<br />![image.png](https://cdn.nlark.com/yuque/0/2019/png/250511/1566532935453-9a34a998-74e3-4291-8d3e-cfdf882da549.png#align=left&display=inline&height=842&name=image.png&originHeight=894&originWidth=1865&size=172625&status=done&width=1756.1206645054087)

- 创建一个index为：**data-http-*  **,我这里是已经创建过一个了。

![image.png](https://cdn.nlark.com/yuque/0/2019/png/250511/1566533079782-fd266d7f-87ef-48aa-afd2-2d7c8698f244.png#align=left&display=inline&height=848&name=image.png&originHeight=901&originWidth=1920&size=172067&status=done&width=1807.9097457642815)
<a name="BM9NA"></a>
# springboot日志系统配置logstash
1、pom中添加依赖

```java
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <artifactId>SpringBoot-Demo-ELK</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>SpringBoot-Demo-ELK</name>
    <description>Demo project for Spring Boot</description>

    <parent>
        <groupId>com.wjwcloud</groupId>
        <artifactId>SpringBoot-Demo</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>net.logstash.logback</groupId>
            <artifactId>logstash-logback-encoder</artifactId>
            <version>5.2</version>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>

        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
        </dependency>

        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>javax.servlet-api</artifactId>
            <!--<version>4.0.1</version>-->
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <!--<optional>true</optional>-->
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.56</version>
        </dependency>
        <!--AOP相关-->
        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjrt</artifactId>
            <version>1.9.2</version>
        </dependency>
        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjweaver</artifactId>
        </dependency>
    </dependencies>

    <build>
        <finalName>spring-boot-demo-ELK</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```
2、application.properties文件中配置如下：

```java
#logstash日志收集地址，即logstash服务器地址
logstash.ip_port=192.168.233.137:5000
#日志保存级别
logging.all.level=info
#日志保存地址，该值与logstash没关系，是当日志存在本地File文件内的文件夹地址
logging.levelfile=/opt/logs/data-center-service
```
3.resources目录下的logback.xml文件配置如下：

```java
<?xml version="1.0" encoding="UTF-8"?>
<configuration debug="false">
    <property resource="application.properties"></property>
    <!--定义日志文件的存储地址 勿在 LogBack 的配置中使用相对路径-->
    <property name="LOG_HOME" value="${logging.levelfile}" />
    <property name="LOG_LEVEL" value="${logging.all.level}" />
    <!-- 控制台输出 -->
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <!--格式化输出：%d表示日期，%thread表示线程名，%-5level：级别从左显示5个字符宽度%msg：日志消息，%n是换行符-->
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50}:%L - %msg  %n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    <!-- 按照每天生成日志文件 -->
    <appender name="FILE"  class="ch.qos.logback.core.rolling.RollingFileAppender">
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!--日志文件输出的文件名-->
            <FileNamePattern>${LOG_HOME}/data-center-service.log.%d{yyyy-MM-dd}.log</FileNamePattern>
            <!--日志文件保留天数-->
            <MaxHistory>30</MaxHistory>
        </rollingPolicy>
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <!--格式化输出：%d表示日期，%thread表示线程名，%-5level：级别从左显示5个字符宽度%msg：日志消息，%n是换行符-->
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <!--日志文件最大的大小-->
        <triggeringPolicy class="ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy">
            <MaxFileSize>100MB</MaxFileSize>
        </triggeringPolicy>
    </appender>
    <appender name="logstash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>${logstash.ip_port}</destination>
        <encoder charset="UTF-8" class="net.logstash.logback.encoder.LogstashEncoder" />
        <queueSize>1048576</queueSize>
        <keepAliveDuration>5 minutes</keepAliveDuration>
        <!--<customFields>{"application-name":"data-repo-interface"}</customFields>-->
        <!--<filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>INFO</level>
        </filter>
        <filter class="ch.qos.logback.core.filter.EvaluatorFilter">
            <evaluator> <!– 默认为 ch.qos.logback.classic.boolex.JaninoEventEvaluator –>
                <expression>return message.contains("billing");</expression>
            </evaluator>
            <OnMatch>ACCEPT</OnMatch>
            <OnMismatch>DENY</OnMismatch>
        </filter>-->
    </appender>

    <logger name="elk_logger" level="INFO" additivity="false">
        <appender-ref ref="logstash"/>
    </logger>

    <!--<logger name="com.wjwcloud" level="${LOG_LEVEL}"/>-->
    <!--<logger name="org.apache.ibatis" level="${LOG_LEVEL}"/>-->
    <!--<logger name="org.mybatis.spring" level="${LOG_LEVEL}"/>-->
    <!--<logger name="org.springframework" level="${LOG_LEVEL}"/>-->
    <!--<logger name="java.sql.Connection" level="${LOG_LEVEL}"/>-->
    <!--<logger name="java.sql.Statement" level="${LOG_LEVEL}"/>-->
    <!--<logger name="java.sql.PreparedStatement" level="${LOG_LEVEL}"/>-->

    <!-- 日志输出级别 -->
    <root level="${LOG_LEVEL}">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="FILE" />
        <!--<appender-ref ref="logstash" />-->
    </root>
</configuration>
```
 <br />上述配置文件中logstash的主要配置：

```java
<appender name="logstash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>${logstash.ip_port}</destination>
        <encoder charset="UTF-8" class="net.logstash.logback.encoder.LogstashEncoder" />
        <queueSize>1048576</queueSize>
        <keepAliveDuration>5 minutes</keepAliveDuration>
        <!--<customFields>{"application-name":"data-repo-interface"}</customFields>-->
        <!--<filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>INFO</level>
        </filter>
        <filter class="ch.qos.logback.core.filter.EvaluatorFilter">
            <evaluator> <!– 默认为 ch.qos.logback.classic.boolex.JaninoEventEvaluator –>
                <expression>return message.contains("billing");</expression>
            </evaluator>
            <OnMatch>ACCEPT</OnMatch>
            <OnMismatch>DENY</OnMismatch>
        </filter>-->
    </appender>

    <logger name="elk_logger" level="INFO" additivity="false">
        <appender-ref ref="logstash"/>
    </logger>
```
其中配置中name=”elk_logger”的指定如下：
```
Logger elkLogger = LoggerFactory.getLogger("elk_logger");
```
当使用slf4j生成Logger时，只要指定Tag为“elk_logger”的日志输出或打印，都会上传到logstash服务器，并存储到elasticsearch，用kibana查看。<br />

<a name="sJWhM"></a>
# 配置kibana
现在只要服务器通过指定的Tag打印日志，日志信息将会上传logstash解析，并且存储到elasticsearch，然后只需要kibana配置对应的elasticsearch的index即可看到所需的日志信息。

通过浏览器访问kibana，http://localhost:5601

<a name="4EE3B"></a>
# 项目工程中关于http接口日志的配置
1.使用了AOP对每次请求进行日志拦截。

定义SysLogAspect类：

```java
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.wjwcloud.elk.commons.utils.SpringContextUtil;
import com.wjwcloud.elk.model.LoggerEntity;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@Slf4j
public class SysLogAspect {

    Logger elkLogger = LoggerFactory.getLogger("elk_logger");

    /**
     * 开始时间
     */
    private long startTime = 0L;
    /**
     * 结束时间
     */
    private long endTime = 0L;

//  @Autowired
//  private ILogService logService;

    public static Map<String, Object> getKeyAndValue(Object obj) {
        Map<String, Object> map = new HashMap<>();
        // 得到类对象
        Class userCla = (Class) obj.getClass();
        /* 得到类中的所有属性集合 */
        Field[] fs = userCla.getFields();
        for (int i = 0; i < fs.length; i++) {
            Field f = fs[i];
            f.setAccessible(true); // 设置些属性是可以访问的
            Object val = new Object();
            try {
                val = f.get(obj);
                // 得到此属性的值
                map.put(f.getName(), val);// 设置键值
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

        }
        return map;
    }

    //通过该注解来判断该接口请求是否进行cut
    @Pointcut("@annotation(com.wjwcloud.elk.aspect.annotation.SysLog)")
    public void cutController() {
    }

    @Before("cutController()")
    public void doBeforeInServiceLayer(JoinPoint joinPoint) {
        log.debug("doBeforeInServiceLayer");
        startTime = System.currentTimeMillis();
    }

    @After("cutController()")
    public void doAfterInServiceLayer(JoinPoint joinPoint) {
        log.debug("doAfterInServiceLayer");
    }

    @Around("cutController()")
    public Object recordSysLog(ProceedingJoinPoint joinPoint) throws Throwable {

        RequestAttributes ra = RequestContextHolder.getRequestAttributes();

        ServletRequestAttributes sra = (ServletRequestAttributes) ra;

        HttpServletRequest request = sra.getRequest();

        HttpServletResponse response = sra.getResponse();

        //ELK日志实体类
        LoggerEntity elkLog = new LoggerEntity();

        //应用程序名称
        elkLog.setApplicationName(SpringContextUtil.getApplicationName());

        //profile.active
       // elkLog.setProfileActive(SpringContextUtil.getActiveProfile());

        // 请求的类名
        elkLog.setClassName(joinPoint.getTarget().getClass().getName());

        // 请求的方法名
        elkLog.setMethodName(joinPoint.getSignature().getName());

        //请求完整地址
        elkLog.setUrl(request.getRequestURL().toString());

        //请求URI
        elkLog.setUri(request.getRequestURI());

        //请求类型
        elkLog.setRequestMethod(request.getMethod());

        String queryString = request.getQueryString();

        Object[] args = joinPoint.getArgs();

        String params = "";

        //获取请求参数集合并进行遍历拼接
        if (args.length > 0) {

            if ("POST".equals(request.getMethod())) {

                //param
                Object object = args[0];

                Map<String, Object> map = new HashMap<>();

                Map paramMap = getKeyAndValue(object);

                map.put("param", paramMap);

                if (args.length > 1) {

                    object = args[1];

                    Map bodyMap = getKeyAndValue(object);

                    map.put("body", bodyMap);
                }

                params = JSON.toJSONStringWithDateFormat(map, "yyyy-MM-dd HH:mm:ss", SerializerFeature.UseSingleQuotes);

            } else if ("GET".equals(request.getMethod())) {

                params = queryString;

            }
        }

        //请求参数内容(param)
        elkLog.setRequestParamData(params);

        //客户端IP
        elkLog.setClientIp(request.getRemoteAddr());

        //终端请求方式,普通请求,ajax请求
        elkLog.setRequestType(request.getHeader("X-Requested-With"));

        //sessionId
        elkLog.setSessionId(request.getRequestedSessionId());

        //请求时间
//        elkLog.setRequestDateTime(new Date(startTime));
        elkLog.setRequestDateTime(new Date());

        Object result = null;

        try {

            // 环绕通知 ProceedingJoinPoint执行proceed方法的作用是让目标方法执行，这也是环绕通知和前置、后置通知方法的一个最大区别。
            result = joinPoint.proceed();

        } catch (Exception e) {

            endTime = System.currentTimeMillis();

            //请求耗时(单位:毫秒)
            elkLog.setSpentTime(endTime - startTime);

            //接口返回时间
            elkLog.setResponseDateTime(new Date(endTime));

            //请求时httpStatusCode代码
            elkLog.setHttpStatusCode(String.valueOf(response.getStatus()));

            String elkLogData = JSON.toJSONStringWithDateFormat(elkLog, "yyyy-MM-dd HH:mm:ss.SSS");

            elkLogger.error(elkLogData);

//            log.error(e.getMessage(), e);

            throw e;

//            return result;

        }

        endTime = System.currentTimeMillis();

        //请求耗时(单位:毫秒)
        elkLog.setSpentTime(endTime - startTime);

        if (!elkLog.getMethodName().equals("getBookOriginalContent")) {

            //接口返回数据
            elkLog.setResponseData(JSON.toJSONStringWithDateFormat(result, "yyyy-MM-dd HH:mm:ss", SerializerFeature.UseSingleQuotes));

        }

        //接口返回时间
        elkLog.setResponseDateTime(new Date(endTime));

        //请求时httpStatusCode代码
        elkLog.setHttpStatusCode(String.valueOf(response.getStatus()));

//        if (SpringContextUtil.getActiveProfile().equals("prod")) {
        String s = JSON.toJSONStringWithDateFormat(elkLog, "yyyy-MM-dd HH:mm:ss.SSS");
        elkLogger.info(s);

        return result;
    }

}
```

SpringContextUtil类:

```java
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * @author JiaweiWu
 */
@Component
public class SpringContextUtil implements ApplicationContextAware {

    private static ApplicationContext context = null;

    /**
     * 传入线程中
     * @param beanName
     * @param <T>
     * @return
     */
    public static <T> T getBean(String beanName) {
        return (T) context.getBean(beanName);
    }

    /**
     * 国际化使用
     * @param key
     * @return
     */
    public static String getMessage(String key) {
        return context.getMessage(key, null, Locale.getDefault());
    }

    /**
     * 获取应用程序名称
     * @return
     */
    public static String getApplicationName() {
        return context.getEnvironment().getProperty("spring.application.name");
    }

    /**
     * 获取当前环境
     * @return
     */
    public static String getActiveProfile() {
        return context.getEnvironment().getActiveProfiles()[0];
    }

    /**(non Javadoc)
     * @Title: setApplicationContext
     * @Description: spring获取bean工具类
     * @param applicationContext applicationContext
     * @throws BeansException BeansException
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        context = applicationContext;
    }
}
```

LoggerEntity类:

```java
package com.wjwcloud.elk.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

//@Entity
//@Table(name = "t_logger_infos")
@Data
public class LoggerEntity implements Serializable {

    /**
     * 应用程序名
     */
    private String applicationName;

    /**
     * spring.profiles.active
     */
    private String profileActive;

    /**
     * 客户端请求ip
     */
    private String clientIp;

    /**
     * 客户端请求路径
     */
    private String uri;

    /**
     * 客户端请求完整路径
     */
    private String url;

    /**
     * 请求方法名
     */
    private String methodName;

    /**
     * 请求类名
     */
    private String className;

    /**
     * 终端请求方式,普通请求,ajax请求
     */
    private String requestType;

    /**
     * 请求方式method,post,get等
     */
    private String requestMethod;

    /**
     * 请求参数内容,json
     */
    private String requestParamData;

    /**
     * 请求body参数内容,json
     */
    private String requestBodyData;

    /**
     * 请求接口唯一session标识
     */
    private String sessionId;

    /**
     * 请求时间
     */
    private Date requestDateTime;

    /**
     * 接口返回时间
     */
    private Date responseDateTime;

    /**
     * 接口返回数据json
     */
    private String responseData;

    /**
     * 请求时httpStatusCode代码，如：200,400,404等
     */
    private String httpStatusCode;

    /**
     * 请求耗时秒单位
     */
    private long spentTime;

}
```

SysLog注解类:

```java
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SysLog {

    boolean isLog() default true;

}
```
在SysLogAspect切面类中使用@SysLog注解来判断该接口是否进行日志传递。<br />所以在我们写的接口中只要在方法上加入该注解，即可控制日志是否上传。<br />
<br />eg:<br />
<br />在某个Controller请求接口上：

```java
@Controller
public class LogstashController {

    @SysLog(isLog = true)
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public String get(@RequestParam String test) {
        return "Success";
    }
}

```

<a name="fRM02"></a>
# Demo源码地址：
GitHub：[https://github.com/wjw0315/SpringBoot-Demo/tree/master/SpringBoot-Demo-ELK](https://github.com/wjw0315/SpringBoot-Demo/tree/master/SpringBoot-Demo-ELK)
