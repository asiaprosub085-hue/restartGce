package com.example.gcerestarter.controller;

import com.example.gcerestarter.model.GceRequest;
import com.example.gcerestarter.model.GceResponse;
import com.example.gcerestarter.service.GceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/gce")
public class GceController {

    private final GceService gceService;

    @Autowired
    public GceController(GceService gceService) {
        this.gceService = gceService;
    }

    /**
     * 重启GCE实例并执行健康检查
     */
    @PostMapping("/restart")
    public ResponseEntity<GceResponse> restartInstance(@Valid @RequestBody GceRequest request) {
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
    @PostMapping("/health-check")
    public ResponseEntity<GceResponse> checkHealth(@Valid @RequestBody GceRequest request) {
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
    