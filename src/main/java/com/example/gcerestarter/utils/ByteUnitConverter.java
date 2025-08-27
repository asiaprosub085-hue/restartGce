package com.example.gcerestarter.utils;
public class ByteUnitConverter {
    // 单位换算常量
    private static final long KB = 1024;
    private static final long MB = 1024 * KB;

    /**
     * 将字节数转换为最合适的单位(KB或MB)并格式化展示
     * 小于1MB的数值用KB展示，大于等于1MB的数值用MB展示
     *
     * @param bytes         字节数(单位: B)
     * @param decimalPlaces 保留的小数位数
     * @return 格式化后的带单位字符串
     */
    public static String convertBytes(long bytes, int decimalPlaces) {
        // 参数校验
        if (bytes < 0) {
            throw new IllegalArgumentException("字节数不能为负数");
        }
        if (decimalPlaces < 0) {
            throw new IllegalArgumentException("小数位数不能为负数");
        }

        // 根据数值大小选择合适的单位
        if (bytes >= MB) {
            // 转换为MB
            double mb = (double) bytes / MB;
            return String.format("%." + decimalPlaces + "f MB", mb);
        } else {
            // 转换为KB
            double kb = (double) bytes / KB;
            return String.format("%." + decimalPlaces + "f KB", kb);
        }
    }

    /**
     * 重载方法，默认保留2位小数
     */
    public static String convertBytes(long bytes) {
        return convertBytes(bytes, 2);
    }

}

