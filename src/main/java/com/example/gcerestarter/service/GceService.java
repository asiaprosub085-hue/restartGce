package com.example.gcerestarter.service;

import com.example.gcerestarter.model.GceRequest;
import com.example.gcerestarter.model.GceResponse;

public interface GceService {

    /**
     * 重启GCE实例并执行健康检查
     */
    GceResponse restartInstance(GceRequest request) throws Exception;

    /**
     * 仅执行健康检查
     */
    GceResponse checkInstanceHealth(GceRequest request) throws Exception;
}
    