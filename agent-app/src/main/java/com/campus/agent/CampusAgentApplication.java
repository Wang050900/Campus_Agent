package com.campus.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.TimeUnit;

/**
 * 校园 AI 助手 — 主启动类
 *
 * 静态块会在 Spring Boot 初始化之前自动杀掉 8080 端口，
 * 无论用 mvn spring-boot:run 还是 java -jar 启动都不用手动清理。
 */
@SpringBootApplication
public class CampusAgentApplication {

    // ======== 一劳永逸：启动前自动释放 8080 端口 ========
    static {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{
                    "powershell", "-Command",
                    "Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue " +
                    "| Stop-Process -Id {$_.OwningProcess} -Force -ErrorAction SilentlyContinue " +
                    "; Write-Output 'Port 8080 released'"
            });
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (finished) {
                String output = new String(process.getInputStream().readAllBytes()).trim();
                System.out.println("🔌 " + output);
            }
        } catch (Exception e) {
            // 杀掉端口失败不影响启动
            System.out.println("🔌 Port 8080 release skipped: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(CampusAgentApplication.class, args);
        System.out.println("========================================");
        System.out.println("  校园 AI 助手已启动！");
        System.out.println("  访问 http://localhost:8080/chat 进行测试");
        System.out.println("========================================");
    }
}
