package com.example.monitoring;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.hc.client5.http.fluent.Content;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WebhookTransformer implements HttpFunction {
    private static final Logger logger = LoggerFactory.getLogger(WebhookTransformer.class);
    private static final Gson gson = new Gson();

    // 目标服务URL - 替换为你的实际目标URL
    private static final String TARGET_URL = "https://gce-traffic-restarter-java-1099221943211.asia-southeast1.run.app/api/gce/restart";

    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException {
        logger.info("Received webhook request from Google Monitoring");

        try {
            // 1. 读取并解析原始请求
            String requestBody = request.getReader().lines()
                    .reduce("", (accumulator, actual) -> accumulator + actual);

            JsonObject monitoringPayload = gson.fromJson(requestBody, JsonObject.class);
            logger.info("Successfully parsed monitoring payload");
            logger.info("monitoringPayload: {}", monitoringPayload.toString());
            // 2. 提取需要的参数
            JsonObject incident = monitoringPayload.getAsJsonObject("incident");
            String alertName = incident != null ?
                    incident.get("policy_name").getAsString() : "Unknown alert";
            String severity = incident != null && incident.has("severity") ?
                    incident.get("severity").getAsString() : "UNKNOWN";
            String resourceName = incident != null && incident.has("resource_display_name") ?
                    incident.get("resource_display_name").getAsString() : "Unknown resource";
            String startTime = incident != null && incident.has("started_at") ?
                    incident.get("started_at").getAsString() : "";

            // 3. 构建自定义参数
            CustomPayload customPayload = new CustomPayload();
            customPayload.setAlertName(alertName);
            customPayload.setSeverity(severity);
            customPayload.setResourceName(resourceName);
            customPayload.setStartTime(startTime);
            customPayload.setSource("Google Monitoring");

            String customPayloadJson = gson.toJson(customPayload);
            logger.info("Custom payload created: {}", customPayloadJson);

            // 4. 转发到目标服务（使用HttpClient 5.x的正确API）
            Content targetResponse = Request.post(TARGET_URL)
                    .bodyString(customPayloadJson, ContentType.APPLICATION_JSON)
                    .execute().returnContent();

            logger.info("Successfully forwarded to target service. Response status: {}", targetResponse.getType());

            // 5. 返回成功响应
            response.setStatusCode(200);
            response.getWriter().write("Payload transformed and forwarded successfully");

        } catch (Exception e) {
            logger.error("Error processing webhook", e);
            response.setStatusCode(500);
            response.getWriter().write("Error processing webhook: " + e.getMessage());
        }
    }

    // 自定义参数模型类
    private static class CustomPayload {
        private String alertName;
        private String severity;
        private String resourceName;
        private String startTime;
        private String source;

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

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }
}
