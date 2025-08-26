package com.example.gcerestarter.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

public class RequestLogger {
    private static final Logger logger = LoggerFactory.getLogger(RequestLogger.class);
    /**
     * 打印HttpServletRequest的详细信息
     */
    public static void logRequest(HttpServletRequest request) {
        logger.error("======= 请求信息开始 =======");

        // 打印请求基本信息
        logger.error("请求URL: " + request.getRequestURL());
        logger.error("请求方法: " + request.getMethod());
        logger.error("请求URI: " + request.getRequestURI());
        logger.error("协议版本: " + request.getProtocol());
        logger.error("客户端IP: " + request.getRemoteAddr());
        logger.error("客户端端口: " + request.getRemotePort());

        // 打印请求头信息
        logger.error("\n======= 请求头信息 =======");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            logger.error(headerName + ": " + headerValue);
        }

        // 打印请求参数信息
        logger.error("\n======= 请求参数信息 =======");
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String paramName = entry.getKey();
            String[] paramValues = entry.getValue();

            if (paramValues.length == 1) {
                logger.error(paramName + ": " + paramValues[0]);
            } else {
                StringBuilder sb = new StringBuilder();
                for (String value : paramValues) {
                    sb.append(value).append(", ");
                }
                logger.error(paramName + ": [" + sb.substring(0, sb.length() - 2) + "]");
            }
        }

        // 打印请求体信息
        logger.error("\n======= 请求体信息 =======");
        try {
            String requestBody = getRequestBody(request);
            logger.error(requestBody);
        } catch (IOException e) {
            logger.error("获取请求体失败: " + e.getMessage());
        }

        logger.error("\n======= 请求信息结束 =======");
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