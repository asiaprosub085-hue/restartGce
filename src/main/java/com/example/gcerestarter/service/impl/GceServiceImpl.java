package com.example.gcerestarter.service.impl;

import com.example.gcerestarter.model.GceRequest;
import com.example.gcerestarter.model.GceResponse;
import com.example.gcerestarter.service.GceService;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.compute.v1.Error;
import com.google.cloud.compute.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
        logger.error("restartInstance 1");
        try (InstancesClient instancesClient = InstancesClient.create()) {
            logger.error("restartInstance 2");
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
            logger.error("实例 {} 当前IP: {}", request.getInstanceName(), originalIp);

            // 执行重启
            logger.error("开始重启实例: {}", request.getInstanceName());
            boolean restartSuccess = restartGceInstance(instancesClient, request);

            if (!restartSuccess) {
                response.setSuccess(false);
                response.setMessage("实例重启命令执行失败");
                return response;
            }

            // 等待实例重启
            logger.error("等待实例重启完成，等待 {} 秒", request.getInitialWaitSeconds());
            TimeUnit.SECONDS.sleep(request.getInitialWaitSeconds());

            // 获取重启后的实例信息
            Instance restartedInstance = getInstance(instancesClient, request);
            String newIp = getExternalIp(restartedInstance);
            response.setNewIp(newIp);
            logger.error("实例 {} 重启后IP: {}", request.getInstanceName(), newIp);

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

    private static final int OPERATION_TIMEOUT_MINUTES = 10; // 延长超时时间至10分钟
    private static final int STATUS_CHECK_INTERVAL_SECONDS = 15; // 状态检查间隔
    // 旧版本客户端库中常见的"完成"状态码（根据实际观察值调整）
    private static final int STATUS_DONE = 1;
    private static final String STATUS_DONE_STR = "DONE";

    /**
     * 重启GCE实例
     */
    private boolean restartGceInstance(InstancesClient instancesClient, GceRequest request) {
        try {

            // 重启前验证实例
            Instance preRestartInstance = getInstance(instancesClient, request);
            if (preRestartInstance == null) {
                logger.error("实例不存在: {}", request.getInstanceName());
                return false;
            }
            String originalIp = getExternalIp(preRestartInstance);
            logger.info("重启前 - 实例状态: {}, IP: {}", preRestartInstance.getStatus(), originalIp);

            // 构建重启请求
            ResetInstanceRequest resetRequest = ResetInstanceRequest.newBuilder()
                    .setProject(request.getProjectId())
                    .setZone(request.getZone())
                    .setInstance(request.getInstanceName())
                    .build();

            // 执行异步重启
            logger.info("发送重启请求: {}", request.getInstanceName());
            OperationFuture<Operation, Operation> future = instancesClient.resetAsync(resetRequest);

            // 等待操作完成（适配无getDone()的旧版本）
            Operation operation = waitForOperationCompletion(future);
            if (operation == null) {
                logger.error("重启操作超时（{}分钟）", OPERATION_TIMEOUT_MINUTES);
                return false;
            }

            // 检查操作是否成功
            if (hasOperationError(operation)) {
                logger.error("重启操作失败 - 错误详情: {}", extractErrorMessage(operation));
                return false;
            }

            // 等待实例运行
            logger.info("重启命令执行完成，等待实例恢复...");
            Instance postRestartInstance = waitForInstanceRunning(instancesClient, request, 600);
            if (postRestartInstance == null) {
                logger.error("实例未进入RUNNING状态");
                return false;
            }

            // 验证IP
            String newIp = getExternalIp(postRestartInstance);
            if (!originalIp.equals(newIp)) {
                logger.warn("IP变化 - 原IP: {}, 新IP: {}", originalIp, newIp);
            } else {
                logger.info("重启成功 - 状态: {}, IP保持不变: {}",
                        postRestartInstance.getStatus(), newIp);
            }

            return true;


        } catch (InterruptedException e) {
            logger.error("操作被中断", e);
            Thread.currentThread().interrupt();
        } finally {
            if (instancesClient != null) {
                instancesClient.close();
            }
        }
        return false;
    }

    /**
     * 等待操作完成（适配无getDone()的情况）
     */
    private Operation waitForOperationCompletion(OperationFuture<Operation, Operation> future)
            throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = TimeUnit.MINUTES.toMillis(OPERATION_TIMEOUT_MINUTES);

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                // 尝试获取操作状态（短超时，避免阻塞）
                Operation operation = future.get(1, TimeUnit.SECONDS);

                // 关键：通过状态码或状态字符串判断是否完成
                if (isOperationDone(operation)) {
                    logger.info("操作完成 - 状态码: {}, 状态描述: {}",
                            operation.getStatus(), getStatusMessage(operation));
                    return operation;
                }

                // 输出中间状态
                logger.info("操作进行中 - 状态码: {}, 描述: {}, 已等待: {}秒",
                        operation.getStatus(),
                        getStatusMessage(operation),
                        (System.currentTimeMillis() - startTime) / 1000);

            } catch (TimeoutException e) {
                // 1秒内未返回，继续等待
            } catch (ExecutionException e) {
                logger.error("操作执行异常", e.getCause());
                return null;
            }

            Thread.sleep(TimeUnit.SECONDS.toMillis(STATUS_CHECK_INTERVAL_SECONDS));
        }

        return null;
    }

    /**
     * 判断操作是否完成（适配旧版本API）
     */
    private boolean isOperationDone(Operation operation) {
        // 方式1：通过状态码判断（根据实际返回的2104194等码调整）
        Operation.Status statusCode = operation.getStatus();
        if (statusCode.getNumber() == STATUS_DONE) {
            return true;
        }

        // 方式2：通过状态字符串判断
        String statusStr = getStatusMessage(operation);
        if (STATUS_DONE_STR.equalsIgnoreCase(statusStr)) {
            return true;
        }

        // 方式3：如果状态码大于某个阈值（根据观察值调整）
        // 例如：某些版本中，状态码>1000000表示完成
        return statusCode.getNumber() > 1000000;
    }

    /**
     * 检查操作是否有错误
     */
    private boolean hasOperationError(Operation operation) {
        // 旧版本可能通过getErrorsCount()判断
        return operation.getError().getErrorsCount() > 0;
    }

    /**
     * 提取错误信息
     */
    private String extractErrorMessage(Operation operation) {
        if (operation.getError().getErrorsCount() == 0) {
            return "未知错误";
        }
        // 拼接所有错误信息
        StringBuilder errorMsg = new StringBuilder();
        Error error = operation.getError();
        errorMsg.append("[").append(error.toString()).append("] ");
        return errorMsg.toString();
    }

    /**
     * 获取状态描述（兼容不同版本的字段名）
     */
    private String getStatusMessage(Operation operation) {
        // 旧版本可能用getOperationType()或getStatus()的字符串表示
        try {
            // 尝试获取状态消息（根据实际API调整）
            return operation.getStatusMessage();
        } catch (Exception e) {
            //  fallback：返回状态码的字符串形式
            return String.valueOf(operation.getStatus());
        }
    }

    // 以下方法与之前相同
    private Instance waitForInstanceRunning(InstancesClient client, GceRequest request, int maxWaitSeconds)
            throws InterruptedException {
        int waitedSeconds = 0;
        while (waitedSeconds < maxWaitSeconds) {
            Instance instance = getInstance(client, request);
            if (instance != null && "RUNNING".equals(instance.getStatus())) {
                return instance;
            }
            Thread.sleep(TimeUnit.SECONDS.toMillis(STATUS_CHECK_INTERVAL_SECONDS));
            waitedSeconds += STATUS_CHECK_INTERVAL_SECONDS;
            logger.info("等待实例运行中 - 已等待: {}秒", waitedSeconds);
        }
        return null;
    }

    /**
     * 获取实例详情
     */
    private Instance getInstance(InstancesClient client, GceRequest request) {
        try {
            return client.get(request.getProjectId(), request.getZone(), request.getInstanceName());
        } catch (Exception e) {
            logger.error("获取实例信息失败", e);
            return null;
        }
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
    