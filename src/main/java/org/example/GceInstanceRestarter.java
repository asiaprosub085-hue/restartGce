//package org.example;
//
//import com.google.api.gax.longrunning.OperationFuture;
//import com.google.cloud.compute.v1.*;
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.net.Socket;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TimeoutException;
//
//public class GceInstanceRestarter {
//    private static final int HEALTH_CHECK_TIMEOUT = 5000; // 5秒
//    private static final int MAX_RETRIES = 5;
//    private static final int INITIAL_WAIT_SECONDS = 30; // 重启后初始等待时间
//    private static final int RETRY_DELAY_SECONDS = 10; // 每次重试间隔
//
//    public static void main(String[] args) {
//        if (args.length < 3) {
//            System.err.println("使用方法: java GceInstanceRestarter <项目ID> <区域> <实例名称> [健康检查端口]");
//            System.err.println("示例: java GceInstanceRestarter my-project us-central1-a my-instance 80");
//            System.exit(1);
//        }
//
//        String projectId = args[0];
//        String zone = args[1];
//        String instanceName = args[2];
//        int healthCheckPort = (args.length > 3) ? Integer.parseInt(args[3]) : 22; // 默认检查SSH端口
//
//        try {
//            // 获取重启前的实例信息
//            Instance instance = getInstance(projectId, zone, instanceName);
//            if (instance == null) {
//                System.err.println("找不到实例: " + instanceName);
//                System.exit(1);
//            }
//
//            String originalIp = getExternalIp(instance);
//            if (originalIp != null) {
//                System.out.println("实例当前外部IP: " + originalIp);
//            }
//
//            // 重启实例
//            System.out.println("正在重启实例: " + instanceName);
//            if (!restartInstance(projectId, zone, instanceName)) {
//                System.err.println("实例重启失败");
//                System.exit(1);
//            }
//
//            // 等待实例重启
//            System.out.println("等待实例重启完成... (" + INITIAL_WAIT_SECONDS + "秒)");
//            TimeUnit.SECONDS.sleep(INITIAL_WAIT_SECONDS);
//
//            // 检查实例状态和健康状况
//            Instance restartedInstance = getInstance(projectId, zone, instanceName);
//            if (restartedInstance == null) {
//                System.err.println("重启后无法获取实例信息");
//                System.exit(1);
//            }
//
//            String newIp = getExternalIp(restartedInstance);
//            if (newIp == null) {
//                System.err.println("无法获取实例外部IP地址");
//                System.exit(1);
//            }
//
//            System.out.println("实例重启后外部IP: " + newIp);
//            System.out.println("开始健康检查 (端口: " + healthCheckPort + ")...");
//
//            // 执行健康检查
//            boolean isHealthy = checkInstanceHealth(newIp, healthCheckPort);
//            if (isHealthy) {
//                System.out.println("实例重启成功并通过健康检查");
//                System.exit(0);
//            } else {
//                System.err.println("实例未通过健康检查");
//                System.exit(1);
//            }
//
//        } catch (Exception e) {
//            System.err.println("操作失败: " + e.getMessage());
//            e.printStackTrace();
//            System.exit(1);
//        }
//    }
//
//    /**
//     * 重启GCE实例
//     */
//    private static boolean restartInstance(String projectId, String zone, String instanceName)
//            throws IOException, ExecutionException, InterruptedException, TimeoutException {
//
//        try (InstancesClient instancesClient = InstancesClient.create()) {
//            ResetInstanceRequest request = ResetInstanceRequest.newBuilder()
//                    .setProject(projectId)
//                    .setZone(zone)
//                    .setInstance(instanceName)
//                    .build();
//
//            OperationFuture<Operation, Operation> future = instancesClient.resetAsync(request);
//            Operation operation = future.get(5, TimeUnit.MINUTES); // 等待操作完成，超时5分钟
//
//            return operation.getStatus().equals("DONE") &&
//                    (operation.getError() == null || operation.getError().getErrorsCount() == 0);
//        }
//    }
//
//    /**
//     * 获取实例详情
//     */
//    private static Instance getInstance(String projectId, String zone, String instanceName) throws IOException {
//        try (InstancesClient instancesClient = InstancesClient.create()) {
//            return instancesClient.get(projectId, zone, instanceName);
//        }
//    }
//
//    /**
//     * 提取实例的外部IP地址
//     */
//    private static String getExternalIp(Instance instance) {
//        for (NetworkInterface iface : instance.getNetworkInterfacesList()) {
//            for (AccessConfig config : iface.getAccessConfigsList()) {
//                if (config.getType().equals("ONE_TO_ONE_NAT")) {
//                    return config.getNatIP();
//                }
//            }
//        }
//        return null;
//    }
//
//    /**
//     * 检查实例健康状态 - 尝试建立TCP连接
//     */
//    private static boolean checkInstanceHealth(String ipAddress, int port) throws InterruptedException {
//        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
//            try (Socket socket = new Socket()) {
//                System.out.println("健康检查尝试 " + attempt + "/" + MAX_RETRIES);
//                socket.connect(new InetSocketAddress(ipAddress, port), HEALTH_CHECK_TIMEOUT);
//                return true; // 连接成功，健康检查通过
//            } catch (IOException e) {
//                System.out.println("健康检查尝试 " + attempt + " 失败: " + e.getMessage());
//                if (attempt < MAX_RETRIES) {
//                    System.out.println("等待 " + RETRY_DELAY_SECONDS + " 秒后重试...");
//                    TimeUnit.SECONDS.sleep(RETRY_DELAY_SECONDS);
//                }
//            }
//        }
//        return false; // 所有尝试都失败
//    }
//}
//
