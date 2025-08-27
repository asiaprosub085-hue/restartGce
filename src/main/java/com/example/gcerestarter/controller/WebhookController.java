package com.example.gcerestarter.controller;

import com.example.gcerestarter.bot.CustomTemplateBot;
import com.example.gcerestarter.service.GceService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

@RestController
public class WebhookController {
    private static final Gson gson = new Gson();
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    @Resource
    CustomTemplateBot customTemplateBot;

    //消息群id
    private static final String chatId = "-1003008517484";
    @Resource
    private GceService gceService;

    // 自定义路径：Monitoring将调用这个路径（例如 /transform）
    @PostMapping("/transform")
    public ResponseEntity<String> handleWebhook(@RequestBody String requestBody) {
        try {
            //解析Monitoring原始请求 monitoringPayload: {"incident":{"condition":{"conditionThreshold":{"aggregations":[{"alignmentPeriod":"300s","perSeriesAligner":"ALIGN_RATE"}],"comparison":"COMPARISON_GT","duration":"0s","filter":"resource.type = \"gce_instance\" AND metric.type = \"compute.googleapis.com/instance/network/sent_bytes_count\" AND metric.labels.instance_name = \"asia-pay-template\"","thresholdValue":2000,"trigger":{"count":1}},"displayName":"VM Instance - Sent bytes","name":"projects/asia-pro-463111/alertPolicies/16449759919842446266/conditions/14514769801551309622"},"condition_name":"VM Instance - Sent bytes","documentation":{"content":"kkkkkkkkkkkkkkkkkkk","mime_type":"text/markdown","subject":"[ALERT - Critical] restartGce"},"ended_at":null,"incident_id":"0.nwg9th6llz4w","metadata":{"system_labels":{},"user_labels":{}},"metric":{"displayName":"Sent bytes","labels":{"loadbalanced":"false"},"type":"compute.googleapis.com/instance/network/sent_bytes_count"},"observed_value":"5072.020","policy_name":"重启Gce","policy_user_labels":{"restart_key":"restart_value"},"resource":{"labels":{"instance_id":"3453121075891706391","project_id":"asia-pro-463111","zone":"asia-southeast1-c"},"type":"gce_instance"},"resource_display_name":"asia-pay-template","resource_id":"","resource_name":"asia-pro-463111 asia-pay-template","resource_type_display_name":"VM Instance","scoping_project_id":"asia-pro-463111","scoping_project_number":1099221943211,"severity":"Critical","started_at":1756293629,"state":"open","summary":"Sent bytes for asia-pro-463111 asia-pay-template with metric labels {loadbalanced=false} is above the threshold of 2000.000 with a value of 5072.020.","threshold_value":"2000","url":"https://console.cloud.google.com/monitoring/alerting/alerts/0.nwg9th6llz4w?channelType=webhook&project=asia-pro-463111"},"version":"1.2"}
            // 1. 解析Monitoring原始请求
            JsonObject monitoringPayload = gson.fromJson(requestBody, JsonObject.class);
            logger.error("解析Monitoring原始请求 monitoringPayload: " + monitoringPayload.toString());
            // 2. 提取参数
            JsonObject incident = monitoringPayload.getAsJsonObject("incident");
            if (null == incident || "".equals(incident)) {
                logger.error("incident 获取异常 ");
                return ResponseEntity.ok("incident 获取异常");
            }
            JsonObject resource = incident.getAsJsonObject("resource");
            if (null == resource || "".equals(resource)) {
                logger.error("resouce 获取异常 ");
                return ResponseEntity.ok("resouce 获取异常");
            }
            JsonObject labels = resource.getAsJsonObject("labels");
            if (null == labels || "".equals(labels)) {
                logger.error("labels 获取异常 ");
                return ResponseEntity.ok("labels 获取异常");
            }
            //实例id
            String instanceId = labels.get("instance_id").getAsString();
            //实例区域
            String zone = labels.get("zone").getAsString();
            //项目id
            String projectId = labels.get("project_id").getAsString();
            //实例名称
            String resourceName = incident.get("resource_display_name").getAsString();
            //限制流量
            String thresholdValue = incident.get("threshold_value").getAsString();
            //实际流量
            String observedValue = incident.get("observed_value").getAsString();
            //任务名称
            String policyName = incident.get("policy_name").getAsString();
            //时间
            String startedAt = incident.get("started_at").getAsString();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            startedAt = dateFormat.format(startedAt);
            //详情
            String url = incident.get("url").getAsString();

            Map<String, String> map = new HashMap<>();
            map.put("instanceId", instanceId);
            map.put("zone", zone);
            map.put("projectId", projectId);
            map.put("resourceName", resourceName);
            map.put("thresholdValue", thresholdValue);
            map.put("observedValue", observedValue);
            map.put("policyName", policyName);
            map.put("startedAt", startedAt);
            map.put("chatId", chatId);
            map.put("url", url);

            logger.error("发送通知: " + map.toString());
            customTemplateBot.sendNoticMessage(map);

            return ResponseEntity.ok("Webhook processed and forwarded");
        } catch (Exception e) {

            logger.error("Error processing webhook 获取异常 " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook: " + e.getMessage());
        }
    }

}
