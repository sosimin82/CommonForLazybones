package com.cfl.service;

import com.cfl.domain.Authority;
import com.cfl.domain.CflObject;
import com.cfl.domain.Code;
import com.cfl.domain.User;
import com.cfl.mapper.MappingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MappingService {

    @Autowired
    private MappingMapper mappingMapper;

    public List<Map<String, String>> getObjectIdAndSubObjectIdMapList(String serviceName, String tenantId) {
        return mappingMapper.selectObjectIdAndSubObjectIdMapList(serviceName, tenantId);
    }

    public List<Map<String, Object>> getObjectIdAndAuthorityMapList(String serviceName, String tenantId) {
        return mappingMapper.selectObjectIdAndAuthorityMapList(serviceName, tenantId);
    }

    public List<Map<String, Object>> getAuthorityIdAndUserMapList(String serviceName, String tenantId) {
        return mappingMapper.selectAuthorityIdAndUserMapList(serviceName, tenantId);
    }

    public boolean isExistObjectAuthorityMapping(String objectId, Authority authority) {
        return mappingMapper.isExistObjectAuthorityMapping(objectId, authority);
    }

    public void createObjectAuthorityMappting(String objectId, Authority authority) {
        mappingMapper.insertObjectAuthority(objectId, authority);
    }

    public void removeObjectAuthorityMapping(String objectId, Authority authority) {
        mappingMapper.deleteObjectAuthority(objectId, authority);
    }

    public void removeObjectMapping(CflObject object) {
        mappingMapper.deleteObjectAuthorityMapping(object);
        mappingMapper.deleteObjectSubObjectMapping(object);
    }

    public boolean isExistAuthorityUserMapping(String authorityId, User user) {
        return mappingMapper.isExistAuthorityUserMapping(authorityId, user);
    }

    public void createAuthorityUserMappting(String authorityId, User user) {
        mappingMapper.insertAuthorityUser(authorityId, user);
    }

    public void removeAuthorityUserMapping(String authorityId, User user) {
        mappingMapper.deleteAuthorityUser(authorityId, user);
    }

    public void removeAuthorityMapping(Authority authority) {
        mappingMapper.deleteAuthorityMapping(authority);
    }

    public List<Authority> getUserAuthorities(User user) {
        return mappingMapper.selectUserAuthorities(user);
    }

    public void createCodeSequenceAndSubCodeSequenceMapping(Long codeSequence, Long subCodeSequence, String treeId, int depth) {
        mappingMapper.insertCodeSequenceAndSubCodeSequenceMapping(codeSequence, subCodeSequence, treeId, depth);
    }
    
    public void removeCodeSequenceAndSubCodeSequenceMapping(Long codeSequence, Long subCodeSequence, String treeId, int depth) {
    	mappingMapper.deleteCodeSequenceAndSubCodeSequenceMapping(codeSequence, subCodeSequence, treeId, depth);
    }

    public List<Code> getTopLevelCodes(String serviceName, String tenantId) {
        return mappingMapper.selectTopLevelCodes(serviceName, tenantId);
    }

    public List<Code> getUsingTopLevelCodes(String serviceName, String tenantId) {
        return mappingMapper.selectUsingTopLevelCodes(serviceName, tenantId);
    }

    public List<Code> getLowLevelCodes(Code highLevelCode, int depth) {
        return mappingMapper.selectLowLevelCodes(highLevelCode, depth);
    }

    public List<Code> getUsingLowLevelCodes(Code highLevelCode, int depth) {
        return mappingMapper.selectUsingLowLevelCodes(highLevelCode, depth);
    }

    public void createObjectSubObjectMapping(String objectId, CflObject object) {
        mappingMapper.insertObjectSubObject(objectId, object);
    }

    public void removeObjectSubObjectMapping(String objectId, CflObject object) {
        mappingMapper.deleteObjectSubObject(objectId, object);
    }

    public boolean isExistObjectSubObjectMapping(String objectId, CflObject object) {
        return mappingMapper.isExistObjectSubObjectMapping(objectId, object);
    }

    public List<String> getTenantSubObjectIdList(String serviceName, String tenantId) {
        return mappingMapper.selectTenantSubObjectIdList(serviceName, tenantId);
    }

    public List<String> getTenantParentObjectIdList(String serviceName, String tenantId) {
        return mappingMapper.selectTenantParentObjectIdList(serviceName, tenantId);
    }
}
