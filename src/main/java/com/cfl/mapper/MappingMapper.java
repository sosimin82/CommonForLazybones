package com.cfl.mapper;

import com.cfl.domain.Authority;
import com.cfl.domain.CflObject;
import com.cfl.domain.Code;
import com.cfl.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MappingMapper {
    List<Map<String, String>> selectObjectIdAndSubObjectIdMapList(@Param("serviceName") String serviceName, @Param("tenantId") String tenantId);
    List<Map<String, Object>> selectObjectIdAndAuthorityMapList(@Param("serviceName") String serviceName, @Param("tenantId") String tenantId);
    List<Map<String, Object>> selectAuthorityIdAndUserMapList(@Param("serviceName") String serviceName, @Param("tenantId") String tenantId);

    boolean isExistObjectAuthorityMapping(@Param("objectId") String objectId, @Param("authority") Authority authority);
    void insertObjectAuthority(@Param("objectId") String objectId, @Param("authority") Authority authority);
    void deleteObjectAuthority(@Param("objectId") String objectId, @Param("authority") Authority authority);
    void deleteObjectMapping(@Param("object") CflObject object);

    boolean isExistAuthorityUserMapping(@Param("authorityId") String authorityId, @Param("user") User user);
    void insertAuthorityUser(@Param("authorityId") String authorityId, @Param("user") User user);
    void deleteAuthorityUser(@Param("authorityId") String authorityId, @Param("user") User user);
    void deleteAuthorityMapping(@Param("authority") Authority authority);

    List<Authority> selectObjectAuthorities(@Param("object") CflObject object);
    List<User> selectAuthorityUsers(@Param("authority") Authority authority);
    List<Authority> selectUserAuthorities(@Param("user") User user);

    void createCodeSequenceAndSubCodeSequenceMapping(Long codeSequence, Long subCodeSequence);
    List<Code> selectTopLevelCodes(String serviceName, String tenantId);
    List<Code> selectUsingTopLevelCodes(String serviceName, String tenantId);
    List<Code> selectLowLevelCodes(Code highLevelCode);
    List<Code> selectUsingLowLevelCodes(Code highLevelCode);
}
