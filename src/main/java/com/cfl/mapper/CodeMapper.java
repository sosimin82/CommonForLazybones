package com.cfl.mapper;

import com.cfl.domain.Code;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CodeMapper {
    void insertCode(@Param("code") Code code);
    void insertCodeMultiLanguage(@Param("multiLanguageCode")  String multiLanguageCode, @Param("multiLanguageMap")  Map<String, String> multiLanguageMap);
    void insertCodeTree(String treeId, Long rootCodeSequence);
    int selectCodeTree(String treeId, Long rootCodeSequence);

    void updateCode(@Param("code") Code code);

    void deleteCode(@Param("code") Code code);
    void deleteCodeMultiLanguage(@Param("multiLanguageCode")  String multiLanguageCode);

    Code selectCode(@Param("code") Code code);
    Integer selectDepth(Long codeSequence, String treeId);

    List<Code> selectAllCodes();
    List<Code> selectServiceCodes(@Param("serviceName") String serviceName);
    List<Code> selectTenantCodes(@Param("serviceName") String serviceName, @Param("tenantId") String tenantId);

    List<Map<String, String>> selectCodeMultiLanguageMapList(@Param("serviceName") String serviceName, @Param("tenantId") String tenantId);
}
