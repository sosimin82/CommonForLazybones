package com.cfl.controller;

import com.cfl.domain.ApiResponse;
import com.cfl.domain.Code;
import com.cfl.service.CodeService;
import com.cfl.util.WildcardPathVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
public class CodeController {
    @Autowired
    private CodeService codeService;

    // urlPath = /{serviceName}/option: {tenantId}/PID/PID/PID/PID/..../ID -> fullIdPath = [{PID},{PID},{PID},....,{ID}]
    private static final String CODE_URL_WITH_TENANT = "/{serviceName}/{tenantId}/code/**";
    private static final String CODE_URL_WITHOUT_TENANT = "/{serviceName}/code/**";

    @PostMapping(value = {CODE_URL_WITH_TENANT, CODE_URL_WITHOUT_TENANT})
    public ApiResponse createCode(@PathVariable("serviceName") String serviceName,
                                  @PathVariable(name = "tenantId", required = false) String tenantId,
                                  @WildcardPathVariable List<String> fullIdPath,
                                  @RequestBody Code code) {
        return codeService.createCode(serviceName, tenantId, fullIdPath, code);
    }

    @PostMapping(value = {"/{serviceName}/{tenantId}/code/mapping", "/{serviceName}/code/mapping"})
    public ApiResponse createCodeMapping(@PathVariable("serviceName") String serviceName,
                                         @PathVariable(name = "tenantId", required = false) String tenantId,
                                         @RequestBody Map<String, Object> codePath) {
        return codeService.createCodeMapping(serviceName, tenantId, codePath);
    }
    
    @DeleteMapping(value = {"/{serviceName}/{tenantId}/code/mapping", "/{serviceName}/code/mapping"})
    public ApiResponse removeCodeMapping(@PathVariable("serviceName") String serviceName,
    		@PathVariable(name = "tenantId", required = false) String tenantId,
    		@RequestBody Map<String, Object> codePath) {
    	return codeService.removeCodeMapping(serviceName, tenantId, codePath);
    }

    @PutMapping(value = {CODE_URL_WITH_TENANT, CODE_URL_WITHOUT_TENANT})
    public ApiResponse modifyCode(@PathVariable("serviceName") String serviceName,
                                  @PathVariable(name = "tenantId", required = false) String tenantId,
                                  @WildcardPathVariable List<String> fullIdPath,
                                  @RequestBody Code code) {
        return codeService.modifyCode(serviceName, tenantId, fullIdPath, code);
    }

    @DeleteMapping(value = {CODE_URL_WITH_TENANT, CODE_URL_WITHOUT_TENANT})
    public ApiResponse removeCode(@PathVariable("serviceName") String serviceName,
                                  @PathVariable(name = "tenantId", required = false) String tenantId,
                                  @WildcardPathVariable List<String> fullIdPath) {
        return codeService.removeCode(serviceName, tenantId, fullIdPath);
    }

    @GetMapping(value = {CODE_URL_WITH_TENANT, CODE_URL_WITHOUT_TENANT})
    public ApiResponse getCode(@PathVariable("serviceName") String serviceName,
                               @PathVariable(name = "tenantId", required = false) String tenantId,
                               @WildcardPathVariable List<String> fullIdPath) {
        return codeService.getCode(serviceName, tenantId, fullIdPath);
    }

    @GetMapping(value = {"/{serviceName}/{tenantId}/using/code/**", "/{serviceName}/{tenantId}/using/code/**"})
    public ApiResponse getUsingCode(@PathVariable("serviceName") String serviceName,
                               @PathVariable(name = "tenantId", required = false) String tenantId,
                               @WildcardPathVariable List<String> fullIdPath) {
        return codeService.getUsingCode(serviceName, tenantId, fullIdPath);
    }

    @GetMapping(value = {"/{serviceName}/{tenantId}/code", "/{serviceName}/code"})
    public ApiResponse getTenantCodes(@PathVariable("serviceName") String serviceName,
                                      @PathVariable(name = "tenantId", required = false) String tenantId) {
        return codeService.getTenantCodeMap(serviceName, tenantId);
    }

    @GetMapping(value = {"/{serviceName}/{tenantId}/using/code", "/{serviceName}/using/code"})
    public ApiResponse getUsingTenantCodes(@PathVariable("serviceName") String serviceName,
                                      @PathVariable(name = "tenantId", required = false) String tenantId) {
        return codeService.getUsingTenantCodeMap(serviceName, tenantId);
    }
}
