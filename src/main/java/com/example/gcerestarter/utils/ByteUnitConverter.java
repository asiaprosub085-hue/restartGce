package com.example.gcerestarter.utils;

public class ByteUnitConverter {

    /**
     * 字节转换为兆字节(MB)
     * 1 MB = 1024 * 1024 B = 1048576 B
     *
     * @param bytes 字节数
     * @return 转换后的兆字节数
     */
    public static double bytesToMB(long bytes) {
        // 防止除以零错误
        if (bytes < 0) {
            throw new IllegalArgumentException("字节数不能为负数");
        }
        return (double) bytes / (1024 * 1024);
    }

    /**
     * 字节转换为兆字节(MB)并格式化输出
     *
     * @param bytes         字节数
     * @param decimalPlaces 保留的小数位数
     * @return 格式化后的MB字符串
     */
    public static String bytesToMBFormatted(long bytes, int decimalPlaces) {
        if (decimalPlaces < 0) {
            throw new IllegalArgumentException("小数位数不能为负数");
        }

        double mb = bytesToMB(bytes);
        // 使用String.format格式化输出
        return String.format("%." + decimalPlaces + "f MB", mb);
    }

    // 示例用法
    public static void main(String[] args) {
        long bytes1 = 2097152; // 2MB
        long bytes2 = 1572864; // 1.5MB

        System.out.println(bytes1 + " B = " + bytesToMB(bytes1) + " MB");
        System.out.println(bytes2 + " B = " + bytesToMBFormatted(bytes2, 2));
    }
}

