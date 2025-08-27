package com.example.gcerestarter.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class WebhookController {
    private static final Gson gson = new Gson();
    // 目标服务URL（替换为你的实际地址）
//    private static final String TARGET_URL = "https://your-target-service.com/webhook";
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    // 自定义路径：Monitoring将调用这个路径（例如 /transform）
    @PostMapping("/transform")
    public ResponseEntity<String> handleWebhook(@RequestBody String requestBody) {
        try {
            // 1. 解析Monitoring原始请求
            JsonObject monitoringPayload = gson.fromJson(requestBody, JsonObject.class);
            logger.error("解析Monitoring原始请求 monitoringPayload: " + monitoringPayload.toString());
            // 2. 提取参数（同之前的逻辑）
            JsonObject incident = monitoringPayload.getAsJsonObject("incident");
            String alertName = incident != null ? incident.get("policy_name").getAsString() : "Unknown alert";
            String severity = incident != null && incident.has("severity") ?
                    incident.get("severity").getAsString() : "UNKNOWN";
            String resourceName = incident != null && incident.has("resource_display_name") ?
                    incident.get("resource_display_name").getAsString() : "Unknown resource";

            // 3. 构建自定义参数
            CustomPayload customPayload = new CustomPayload();
            customPayload.setAlertName(alertName);
            customPayload.setSeverity(severity);
            customPayload.setResourceName(resourceName);

            // 4. 转发到目标服务
//            restTemplate.postForObject(TARGET_URL, customPayload, String.class);
            logger.error("转发到目标服务 customPayload: " + customPayload.toString());
            return ResponseEntity.ok("Webhook processed and forwarded");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook: " + e.getMessage());
        }
    }

    // 自定义参数模型
    public static class CustomPayload {
        private String alertName;
        private String severity;
        private String resourceName;

        // Getters and Setters
        public String getAlertName() {
            return alertName;
        }

        public void setAlertName(String alertName) {
            this.alertName = alertName;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getResourceName() {
            return resourceName;
        }

        public void setResourceName(String resourceName) {
            this.resourceName = resourceName;
        }

        @Override
        public String toString() {
            return "CustomPayload{" +
                    "alertName='" + alertName + '\'' +
                    ", severity='" + severity + '\'' +
                    ", resourceName='" + resourceName + '\'' +
                    '}';
        }
    }
}
