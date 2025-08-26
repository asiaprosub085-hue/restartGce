package com.example.gcerestarter.utils;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

public class RequestLogger {

    /**
     * 打印HttpServletRequest的详细信息
     */
    public static void logRequest(HttpServletRequest request) {
        System.out.println("======= 请求信息开始 =======");

        // 打印请求基本信息
        System.out.println("请求URL: " + request.getRequestURL());
        System.out.println("请求方法: " + request.getMethod());
        System.out.println("请求URI: " + request.getRequestURI());
        System.out.println("协议版本: " + request.getProtocol());
        System.out.println("客户端IP: " + request.getRemoteAddr());
        System.out.println("客户端端口: " + request.getRemotePort());

        // 打印请求头信息
        System.out.println("\n======= 请求头信息 =======");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            System.out.println(headerName + ": " + headerValue);
        }

        // 打印请求参数信息
        System.out.println("\n======= 请求参数信息 =======");
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String paramName = entry.getKey();
            String[] paramValues = entry.getValue();

            if (paramValues.length == 1) {
                System.out.println(paramName + ": " + paramValues[0]);
            } else {
                StringBuilder sb = new StringBuilder();
                for (String value : paramValues) {
                    sb.append(value).append(", ");
                }
                System.out.println(paramName + ": [" + sb.substring(0, sb.length() - 2) + "]");
            }
        }

        // 打印请求体信息
        System.out.println("\n======= 请求体信息 =======");
        try {
            String requestBody = getRequestBody(request);
            System.out.println(requestBody);
        } catch (IOException e) {
            System.out.println("获取请求体失败: " + e.getMessage());
        }

        System.out.println("\n======= 请求信息结束 =======");
    }

    /**
     * 获取请求体内容
     */
    private static String getRequestBody(HttpServletRequest request) throws IOException {
        BufferedReader reader = request.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}