package com.campus.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 校园 AI 助手 — 主启动类
 *
 * 这就是整个应用的入口，Spring Boot 会：
 * 1. 自动扫描 @Component、@Service、@Controller 等注解
 * 2. 自动加载 application.yml 配置
 * 3. 启动内嵌的 Tomcat 服务器
 */
@SpringBootApplication
public class CampusAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusAgentApplication.class, args);
        System.out.println("========================================");
        System.out.println("  校园 AI 助手已启动！");
        System.out.println("  访问 http://localhost:8080/chat 进行测试");
        System.out.println("========================================");
    }
}
