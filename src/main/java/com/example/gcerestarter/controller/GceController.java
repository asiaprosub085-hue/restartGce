package com.example.gcerestarter.controller;

import com.example.gcerestarter.model.GceRequest;
import com.example.gcerestarter.model.GceResponse;
import com.example.gcerestarter.service.GceService;
import com.example.gcerestarter.utils.RequestLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/gce")
public class GceController {

    private final GceService gceService;

    @Autowired
    public GceController(GceService gceService) {
        this.gceService = gceService;
    }

    private static final Logger logger = LoggerFactory.getLogger(GceController.class);

    /**
     * 重启GCE实例并执行健康检查
     */
    @PostMapping("/restart")
    public ResponseEntity<GceResponse> restartInstance(HttpServletRequest httpServletRequest, @Valid @RequestBody GceRequest request) {
        try {
            RequestLogger.logRequest(httpServletRequest);
            GceResponse response = gceService.restartInstance(request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            GceResponse errorResponse = new GceResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("重启实例失败: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * 重启GCE实例并执行健康检查
     */
    @GetMapping("/restartGet")
    public ResponseEntity<GceResponse> restartInstanceGet(GceRequest request) {
        try {
            GceResponse response = gceService.restartInstance(request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            GceResponse errorResponse = new GceResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("重启实例失败: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * 单独执行健康检查（不重启实例）
     */
    @GetMapping("/health-check")
    public ResponseEntity<GceResponse> checkHealth(GceRequest request) {
        try {
            GceResponse response = gceService.checkInstanceHealth(request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            GceResponse errorResponse = new GceResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("健康检查失败: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
    