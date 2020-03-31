package com.cfl.service;

import com.cfl.cache.Cache;
import com.cfl.domain.ApiResponse;
import com.cfl.domain.CacheUpdateRequest;
import com.cfl.domain.Code;
import com.cfl.mapper.CodeMapper;
import com.cfl.util.ApiResponseUtil;
import com.cfl.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class CodeService{
    @Autowired
    private CodeMapper codeMapper;
    @Autowired
    private NetworkService networkService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private MappingService mappingService;

    public List<Code> getAllCodes() {
        return codeMapper.selectAllCodes();
    }

    public List<Code> getServiceCodes(String serviceName) {
        return codeMapper.selectServiceCodes(serviceName);
    }

    public List<Code> getTenantCodes(String serviceName, String tenantId) {
        return codeMapper.selectTenantCodes(serviceName, tenantId);
    }

    public List<Map<String, String>> getCodeMultiLanguageMapList(String serviceName, String tenantId) {
        return codeMapper.selectCodeMultiLanguageMapList(serviceName, tenantId);
    }

    /***
     * 코드 생성 후 full id path 로 온 경로에 매핑한다.
     * 코드 연결에 문제가 있는 경우 생성하지 않고 code mapping error를 반환한다.
     */
    public ApiResponse createCode(String serviceName, String tenantId, List<String> fullIdPath, Code code) {
        ApiResponse apiResponse;

        try {
            code.setServiceName(serviceName);
            code.setTenantId(tenantId);
            // fullIdPath last element = codeId
            code.setCodeId(fullIdPath.get(fullIdPath.size() - 1));
            code.setMultiLanguageCode(UUID.randomUUID().toString());
            
            // 다국어를 사용하지 않는 코드의 경우 다국어중 한국어(ko)만 기본으로 하여 다국어 맵 생성
            if (code.getMultiLanguageMap() == null) {
            	code.setMultiLanguageMap(this.createDefaultMultiLanguageCode(code.getCodeName()));
            }

            if (isExistCode(code, fullIdPath)) {
                apiResponse = ApiResponseUtil.getDuplicateCreationApiResponse();
            } else {
                // 코드생성 -> 1. 최상위 코드인 경우 해당 최상위 코드의 코드 트리 생성 2. 하위 코드인 경우 해당 코드트리에 코드 매핑 추가 -> 동기화 진행
                if (fullIdPath.size() == 1 && !"".equals(fullIdPath.get(0))) {
                    codeMapper.insertCode(code);
                    codeMapper.insertCodeMultiLanguage(code.getMultiLanguageCode(), code.getMultiLanguageMap());
                    codeMapper.insertCodeTree(Constant.TREE_ID_PREFIX + code.getCodeSequence(), code.getCodeSequence());
                    networkService.sendProvideServersToInit("cfl", new CacheUpdateRequest(serviceName, tenantId, "code"));
                    apiResponse = ApiResponseUtil.getSuccessApiResponse(code);
                } else {
                    String highLevelFullDepth = getHighLevelFullDepth(fullIdPath);
                    long highLevelCodeSequence = getCodeSequenceFromCache(code, highLevelFullDepth);
                    long rootCodeSequence = getCodeSequenceFromCache(code, getRootFullDepth(fullIdPath));
                    String treeId = Constant.TREE_ID_PREFIX + rootCodeSequence;

                    // 코드 연결에 문제가 있는 경우 (상위 코드 시퀀스가 없거나 상위 코드 full depth가 잘못된 경우)
                    if (rootCodeSequence == -1 || highLevelCodeSequence == -1 || "".equals(highLevelFullDepth)) {
                        apiResponse = ApiResponseUtil.getCodeMappingErrorApiResponse();
                    } else {
                        codeMapper.insertCode(code);
                        codeMapper.insertCodeMultiLanguage(code.getMultiLanguageCode(), code.getMultiLanguageMap());
                        mappingService.createCodeSequenceAndSubCodeSequenceMapping(highLevelCodeSequence, code.getCodeSequence(), treeId, getCodeDepth(highLevelFullDepth));
                        networkService.sendProvideServersToInit("cfl", new CacheUpdateRequest(serviceName, tenantId, "code"));
                        apiResponse = ApiResponseUtil.getSuccessApiResponse(code);
                    }
                }
            }
        } catch (Exception e) {
            log.error("createCode fail", e);
            apiResponse = ApiResponseUtil.getFailureApiResponse();
        }

        historyService.createHistory(serviceName, code.getTenantId(), code, apiResponse);
        return apiResponse;
    }

    public ApiResponse createCodeMapping(String serviceName, String tenantId, Map<String, Object> codePath) {
        ApiResponse apiResponse;
        Code code = new Code(serviceName, tenantId);
        String codeFullIdPath = MapUtils.getString(codePath, "codeFullIdPath");
        String subCodeFullIdPath = MapUtils.getString(codePath, "subCodeFullIdPath");
        boolean isRoot = MapUtils.getBoolean(codePath, "isRoot");

        long rootCodeSequence = getRootCodeSequence(isRoot, code, codeFullIdPath);
        String treeId = Constant.TREE_ID_PREFIX + rootCodeSequence;

        String[] subCodeFullIdPathArray = subCodeFullIdPath.split(":");
        String subCodeId = subCodeFullIdPathArray[subCodeFullIdPathArray.length - 1] + ":";

//        rootCodeSequence가 존재하지 않거나 코드매핑이 이미 존재하는 경우 매핑 추가 에러
        if (rootCodeSequence == -1 || getCodeFromCache(code, codeFullIdPath + subCodeId) != null) {
            apiResponse = ApiResponseUtil.getCodeMappingErrorApiResponse();
        } else {
            long codeSequence = getCodeSequenceFromCache(code, codeFullIdPath);
            long subCodeSequence = getCodeSequenceFromCache(code, subCodeFullIdPath);

            if (isRoot && codeMapper.selectCodeTree(treeId, rootCodeSequence) == 0) {
           		codeMapper.insertCodeTree(treeId, rootCodeSequence);
            }

            try {
            	int depth = getDepthByRoot(isRoot, codeFullIdPath);
                mappingService.createCodeSequenceAndSubCodeSequenceMapping(codeSequence, subCodeSequence, treeId, depth);
                apiResponse = ApiResponseUtil.getSuccessApiResponse(code);

            } catch (Exception e) {
                log.error("createCodeMapping fail", e);
                apiResponse = ApiResponseUtil.getFailureApiResponse();
            }
        }

        historyService.createHistory(serviceName, tenantId, code, apiResponse);
        return apiResponse;
    }
    
    public ApiResponse removeCodeMapping(String serviceName, String tenantId, Map<String, Object> codePath) {
        ApiResponse apiResponse;
        Code code = new Code(serviceName, tenantId);
        String codeFullIdPath = MapUtils.getString(codePath, "codeFullIdPath");
        String subCodeFullIdPath = MapUtils.getString(codePath, "subCodeFullIdPath");
        boolean isRoot = MapUtils.getBoolean(codePath, "isRoot");
        
        long rootCodeSequence = getRootCodeSequence(isRoot, code, codeFullIdPath);
        String treeId = Constant.TREE_ID_PREFIX + rootCodeSequence;
        
        long codeSequence = getCodeSequenceFromCache(code, codeFullIdPath);
        long subCodeSequence = getCodeSequenceFromCache(code, subCodeFullIdPath);
        try {
        	int depth = getDepthByRoot(isRoot, codeFullIdPath);
            mappingService.removeCodeSequenceAndSubCodeSequenceMapping(codeSequence, subCodeSequence, treeId, depth);
            apiResponse = ApiResponseUtil.getSuccessApiResponse(code);
        } catch (Exception e) {
            log.error("createCodeMapping fail", e);
            apiResponse = ApiResponseUtil.getFailureApiResponse();
        }
    	
    	return apiResponse;
    }
    
    private long getRootCodeSequence(boolean isRoot, Code code, String codeFullIdPath) {
        long rootCodeSequence;
        if (isRoot) {
            rootCodeSequence = getCodeSequenceFromCache(code, codeFullIdPath);
        } else {
            rootCodeSequence = getCodeSequenceFromCache(code, getRootFullDepth(codeFullIdPath));
        }
    	return rootCodeSequence;
    }
    
    
    private int getDepthByRoot(boolean isRoot, String codeFullIdPath) {
    	int depth = 1;
    	if (!isRoot) {
    		depth = getCodeDepth(codeFullIdPath);
    	}
    	return depth;
    }
    
    public ApiResponse modifyCode(String serviceName, String tenantId, List<String> fullIdPath, Code code) {
        ApiResponse apiResponse;

        try {
            code.setServiceName(serviceName);
            code.setTenantId(tenantId);

            Code codeFromCache = getCodeFromCache(code, getFullDepth(fullIdPath));

            // 존재하지 않는 코드을 수정하려는 경우
            if (codeFromCache == null) {
                apiResponse = ApiResponseUtil.getMissingValueApiResponse();
            } else {
            	// 코드 시퀀스와 다국어 코드 값은 기존에 등록된 다국어 코드 값을 활용
                code.setCodeSequence(codeFromCache.getCodeSequence());
                code.setMultiLanguageCode(codeFromCache.getMultiLanguageCode());
                codeMapper.updateCode(code);
                
                if (code.getMultiLanguageMap() != null) {
                	// 다국어 정보의 경우 기존 데이터를 지우고 새로 생성한다.
                	codeMapper.deleteCodeMultiLanguage(code.getMultiLanguageCode());
                	codeMapper.insertCodeMultiLanguage(code.getMultiLanguageCode(), code.getMultiLanguageMap());
                }

                networkService.sendProvideServersToInit("cfl", new CacheUpdateRequest(serviceName, tenantId , "code"));
                apiResponse = ApiResponseUtil.getSuccessApiResponse(code);
            }
        } catch(Exception e) {
            log.error("modifyCode fail", e);
            apiResponse = ApiResponseUtil.getFailureApiResponse();
        }

        historyService.createHistory(serviceName, code.getTenantId(), code, apiResponse);
        return apiResponse;
    }

    public ApiResponse removeCode(String serviceName, String tenantId, List<String> fullIdPath) {
        ApiResponse apiResponse;
        Code code = new Code(serviceName, tenantId);

        try {
            Code codeFromCache = getCodeFromCache(code, getFullDepth(fullIdPath));

            // 존재하지 않는 코드을 삭제하려는 경우
            if (codeFromCache == null) {
                apiResponse = ApiResponseUtil.getMissingValueApiResponse();
            } else {
                code.setCodeSequence(code.getCodeSequence());

                codeMapper.deleteCodeMultiLanguage(code.getMultiLanguageCode());
                codeMapper.deleteCode(code);

                networkService.sendProvideServersToInit("cfl", new CacheUpdateRequest(serviceName, tenantId , "code"));
                apiResponse = ApiResponseUtil.getSuccessApiResponse(code);
            }
        } catch (Exception e) {
            log.error("removeCode fail", e);
            apiResponse = ApiResponseUtil.getFailureApiResponse();
        }

        historyService.createHistory(serviceName, code.getTenantId(), code, apiResponse);
        return apiResponse;
    }

    public ApiResponse getCode(String serviceName, String tenantId, List<String> fullIdPath) {
        ApiResponse apiResponse;
        Code code = new Code(serviceName, tenantId);

        try {
            Code codeFromCache = getCodeFromCache(code, getFullDepth(fullIdPath));

            // 캐시에 코드가 없는 경우
            if (codeFromCache == null) {
                apiResponse = ApiResponseUtil.getMissingValueApiResponse();
            } else {
                apiResponse = ApiResponseUtil.getSuccessApiResponse(codeFromCache);
            }
        } catch (Exception e) {
            log.error("getCode fail", e);
            apiResponse = ApiResponseUtil.getFailureApiResponse();
        }

        return apiResponse;
    }

    public ApiResponse getUsingCode(String serviceName, String tenantId, List<String> fullIdPath) {
        ApiResponse apiResponse;
        Code code = new Code(serviceName, tenantId);

        try {
            Code usingCodeFromCache = getUsingCodeFromCache(code, getFullDepth(fullIdPath));

            // 캐시에 코드가 없는 경우
            if (usingCodeFromCache == null) {
                apiResponse = ApiResponseUtil.getMissingValueApiResponse();
            } else {
                apiResponse = ApiResponseUtil.getSuccessApiResponse(usingCodeFromCache);
            }
        } catch (Exception e) {
            log.error("getCode fail", e);
            apiResponse = ApiResponseUtil.getFailureApiResponse();
        }

        return apiResponse;
    }

    public ApiResponse getTenantCodeMap(String serviceName, String tenantId) {
        ApiResponse apiResponse;
        Code code = new Code(serviceName, tenantId);

        try {
            Map<String, Code> codeMap = getTenantCodeMapFromCache(code);

            if (codeMap != null) {
                apiResponse = ApiResponseUtil.getSuccessApiResponse(codeMap);
            } else {
                apiResponse = ApiResponseUtil.getSuccessApiResponse(new HashMap<String, Code>());
            }
        } catch (Exception e) {
            log.error("getTenantCodeList fail", e);
            apiResponse = ApiResponseUtil.getFailureApiResponse();
        }

        return apiResponse;
    }

    public ApiResponse getUsingTenantCodeMap(String serviceName, String tenantId) {
        ApiResponse apiResponse;
        Code code = new Code(serviceName, tenantId);

        try {
            Map<String, Code> codeMap = getTenantUsingCodeMapFromCache(code);

            if (codeMap != null) {
                apiResponse = ApiResponseUtil.getSuccessApiResponse(codeMap);
            } else {
                apiResponse = ApiResponseUtil.getSuccessApiResponse(new HashMap<String, Code>());
            }
        } catch (Exception e) {
            log.error("getTenantCodeList fail", e);
            apiResponse = ApiResponseUtil.getFailureApiResponse();
        }

        return apiResponse;
    }

    private boolean isExistCode(Code code, List<String> fullIdPath) {
        String fullDepth = getFullDepth(fullIdPath);
        if (getCodeFromCache(code, fullDepth) != null) {
            return true;
        }
        return false;
    }

    // urlPath = PID/PID/PID/PID/..../ID -> fullIdPath = [{PID},{PID},{PID},....,{ID}]
    // fullDepth = PID:PID:PID:...:ID
    private String getFullDepth(List<String> fullIdPath) {
        StringBuilder fullDepth = new StringBuilder();

        for (String codeId : fullIdPath) {
            fullDepth.append(codeId).append(":");
        }

        return fullDepth.toString();
    }

    private String getRootFullDepth(List<String> fullIdPath) {
        StringBuilder rootFullDepth = new StringBuilder();

        String rootId = fullIdPath.get(0);
        rootFullDepth.append(rootId).append(":");

        return rootFullDepth.toString();
    }

    private String getRootFullDepth(String fullIdPath) {
        StringBuilder rootFullDepth = new StringBuilder();

        String rootId = fullIdPath.split(":")[0];
        rootFullDepth.append(rootId).append(":");

        return rootFullDepth.toString();
    }

    private String getHighLevelFullDepth(List<String> fullIdPath) {
        StringBuilder highLevelFullDepth = new StringBuilder();

        int fullIdPathLastIndex = fullIdPath.size() - 1;

        for (String codeId : fullIdPath) {
            if (fullIdPathLastIndex != fullIdPath.indexOf(codeId)) {
                highLevelFullDepth.append(codeId).append(":");
            }
        }

        return highLevelFullDepth.toString();
    }

    private Map<String, Code> getTenantCodeMapFromCache(Code code) {
        // 캐시에서 찾는 맵이 없는 경우 null 반환
        Map<String, Map<String, Code>> serviceMapFromCache = Cache.codeCache.get(code.getServiceName());
        if (serviceMapFromCache == null) {
            return null;
        }

        return serviceMapFromCache.get(code.getTenantId());
    }

    private Code getCodeFromCache(Code code, String getFullDepth) {
        // 캐시에서 찾는 코드가 없는 경우 null 반환
        Map<String, Code> tenantMapFromCache = getTenantCodeMapFromCache(code);
        if (tenantMapFromCache == null) {
            return null;
        }

        return tenantMapFromCache.get(getFullDepth);
    }

    private long getCodeSequenceFromCache(Code code, String getFullDepth) {
        Code codeFromCache = getCodeFromCache(code, getFullDepth);
        // 캐시에서 찾는 코드 시퀀스가 없는 경우 -1 반환
        if (codeFromCache == null) {
            return -1;
        }

        return codeFromCache.getCodeSequence();
    }

    private int getCodeDepth(String getFullDepth) {
        return getFullDepth.split(":").length;
    }

    private Map<String, Code> getTenantUsingCodeMapFromCache(Code code) {
        // 캐시에서 찾는 맵이 없는 경우 null 반환
        Map<String, Map<String, Code>> serviceMapFromCache = Cache.usingCodeCache.get(code.getServiceName());
        if (serviceMapFromCache == null) {
            return null;
        }

        return serviceMapFromCache.get(code.getTenantId());
    }

    private Code getUsingCodeFromCache(Code code, String getFullDepth) {
        // 캐시에서 찾는 코드가 없는 경우 null 반환
        Map<String, Code> tenantMapFromCache = getTenantUsingCodeMapFromCache(code);
        if (tenantMapFromCache == null) {
            return null;
        }

        return tenantMapFromCache.get(getFullDepth);
    }
    
    private Map<String, String> createDefaultMultiLanguageCode(String codeName) {
    	Map<String, String> multiLanguageMap = new HashMap<String, String>();
    	multiLanguageMap.put("ko", codeName);
    	return multiLanguageMap;
    }
}
