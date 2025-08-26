package com.example.gcerestarter.service.impl;

import com.example.gcerestarter.model.GceRequest;
import com.example.gcerestarter.model.GceResponse;
import com.example.gcerestarter.service.GceService;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.compute.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class GceServiceImpl implements GceService {

    private static final Logger logger = LoggerFactory.getLogger(GceServiceImpl.class);

    @Override
    public GceResponse restartInstance(GceRequest request) throws Exception {
        GceResponse response = new GceResponse();
        response.setInstanceName(request.getInstanceName());
        response.setTimestamp(LocalDateTime.now());

        try (InstancesClient instancesClient = InstancesClient.create()) {
            // 获取重启前的实例信息
            Instance instance = getInstance(instancesClient, request);
            if (instance == null) {
                response.setSuccess(false);
                response.setMessage("实例不存在: " + request.getInstanceName());
                return response;
            }

            // 记录原始IP
            String originalIp = getExternalIp(instance);
            response.setOriginalIp(originalIp);
            logger.info("实例 {} 当前IP: {}", request.getInstanceName(), originalIp);

            // 执行重启
            logger.info("开始重启实例: {}", request.getInstanceName());
            boolean restartSuccess = restartGceInstance(instancesClient, request);

            if (!restartSuccess) {
                response.setSuccess(false);
                response.setMessage("实例重启命令执行失败");
                return response;
            }

            // 等待实例重启
            logger.info("等待实例重启完成，等待 {} 秒", request.getInitialWaitSeconds());
            TimeUnit.SECONDS.sleep(request.getInitialWaitSeconds());

            // 获取重启后的实例信息
            Instance restartedInstance = getInstance(instancesClient, request);
            String newIp = getExternalIp(restartedInstance);
            response.setNewIp(newIp);
            logger.info("实例 {} 重启后IP: {}", request.getInstanceName(), newIp);

            // 执行健康检查
            boolean healthCheckPassed = checkHealth(newIp, request.getHealthCheckPort(),
                    request.getMaxRetries(), request.getRetryDelaySeconds());
            response.setHealthCheckPassed(healthCheckPassed);

            if (healthCheckPassed) {
                response.setSuccess(true);
                response.setMessage("实例重启成功并通过健康检查");
            } else {
                response.setSuccess(false);
                response.setMessage("实例重启成功但未通过健康检查");
            }

            return response;
        }
    }

    @Override
    public GceResponse checkInstanceHealth(GceRequest request) throws Exception {
        GceResponse response = new GceResponse();
        response.setInstanceName(request.getInstanceName());
        response.setTimestamp(LocalDateTime.now());

        try (InstancesClient instancesClient = InstancesClient.create()) {
            // 获取实例信息
            Instance instance = getInstance(instancesClient, request);
            if (instance == null) {
                response.setSuccess(false);
                response.setMessage("实例不存在: " + request.getInstanceName());
                return response;
            }

            // 获取当前IP
            String currentIp = getExternalIp(instance);
            response.setOriginalIp(currentIp); // 在健康检查中使用originalIp字段存储当前IP
            logger.info("对实例 {} (IP: {}) 执行健康检查", request.getInstanceName(), currentIp);

            // 执行健康检查
            boolean healthCheckPassed = checkHealth(currentIp, request.getHealthCheckPort(),
                    request.getMaxRetries(), request.getRetryDelaySeconds());
            response.setHealthCheckPassed(healthCheckPassed);

            if (healthCheckPassed) {
                response.setSuccess(true);
                response.setMessage("健康检查通过");
            } else {
                response.setSuccess(false);
                response.setMessage("健康检查未通过");
            }

            return response;
        }
    }

    /**
     * 获取GCE实例详情
     */
    private Instance getInstance(InstancesClient client, GceRequest request) throws IOException {
        try {
            return client.get(request.getProjectId(), request.getZone(), request.getInstanceName());
        } catch (Exception e) {
            logger.error("获取实例信息失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 提取实例的外部IP地址
     */
    private String getExternalIp(Instance instance) {
        if (instance == null || instance.getNetworkInterfacesList() == null) {
            return null;
        }

        for (NetworkInterface iface : instance.getNetworkInterfacesList()) {
            if (iface.getAccessConfigsList() != null) {
                for (AccessConfig config : iface.getAccessConfigsList()) {
                    if ("ONE_TO_ONE_NAT".equals(config.getType())) {
                        return config.getNatIP();
                    }
                }
            }
        }
        return null;
    }

    /**
     * 重启GCE实例
     */
    private boolean restartGceInstance(InstancesClient client, GceRequest request)
            throws InterruptedException, ExecutionException, TimeoutException {

        ResetInstanceRequest resetRequest = ResetInstanceRequest.newBuilder()
                .setProject(request.getProjectId())
                .setZone(request.getZone())
                .setInstance(request.getInstanceName())
                .build();

        OperationFuture<Operation, Operation> future = client.resetAsync(resetRequest);
        Operation operation = future.get(5, TimeUnit.MINUTES); // 5分钟超时

        return operation.getStatus().equals("DONE") &&
                (operation.getError() == null || operation.getError().getErrorsCount() == 0);
    }

    /**
     * 执行健康检查（TCP连接测试）
     */
    private boolean checkHealth(String ipAddress, int port, int maxRetries, int retryDelaySeconds)
            throws InterruptedException {

        if (ipAddress == null || ipAddress.isEmpty()) {
            logger.error("IP地址为空，无法执行健康检查");
            return false;
        }

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try (Socket socket = new Socket()) {
                logger.info("健康检查尝试 {}/{} - 连接到 {}:{}",
                        attempt, maxRetries, ipAddress, port);

                socket.connect(new InetSocketAddress(ipAddress, port), 5000); // 5秒超时
                logger.info("健康检查通过 - {}:{}", ipAddress, port);
                return true;
            } catch (Exception e) {
                logger.warn("健康检查尝试 {} 失败: {}", attempt, e.getMessage());

                if (attempt < maxRetries) {
                    logger.info("等待 {} 秒后重试...", retryDelaySeconds);
                    TimeUnit.SECONDS.sleep(retryDelaySeconds);
                }
            }
        }

        logger.error("所有 {} 次健康检查尝试均失败 - {}:{}", maxRetries, ipAddress, port);
        return false;
    }
}
    