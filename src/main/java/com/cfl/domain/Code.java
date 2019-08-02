package com.cfl.domain;

import com.cfl.util.Constant;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class Code {
    private Long codeSequence;
    private String codeId;
    private String codeName;
    private String codeDescription;
    private Boolean isUsed;
    private int order;
    private String multiLanguageCode;
    private String fullSequencePath;
    private String tenantId;
    private String serviceName;
    
    private Map<String, Code> subCodes;
    private Map<String, String> multiLanguageMap;

    public Code() { }

    public Code(Long codeSequence) {
        this.codeSequence = codeSequence;
    }
    public Code(String serviceName, String tenantId) {
        this.serviceName = serviceName;

        if (tenantId == null) {
            this.tenantId = Constant.DEFAULT_TENANT_ID;
        } else {
            this.tenantId = tenantId;
        }
    }

    public Code(String serviceName, String tenantId, String codeId) {
        this(serviceName, tenantId);
        this.codeId = codeId;
    }

    public Code(String serviceName, String tenantId, Long codeSequence) {
        this(serviceName, tenantId);
        this.codeSequence = codeSequence;
    }

    public void setTenantId(String tenantId) {
        if (tenantId == null) {
            this.tenantId = Constant.DEFAULT_TENANT_ID;
        } else {
            this.tenantId = tenantId;
        }
    }
}
