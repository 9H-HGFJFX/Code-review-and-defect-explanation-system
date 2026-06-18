package com.codeaudit.service;

import com.codeaudit.dto.RuleSaveReq;
import com.codeaudit.entity.Rule;

import java.util.List;

public interface RuleService {
    List<Rule> listEnabled();
    List<Rule> listAll(int current, int size, String category);
    long countAll(String category);
    Rule getById(Long id);
    Long add(RuleSaveReq req, String currentRole);
    void update(Long id, RuleSaveReq req, String currentRole);
    void delete(Long id, String currentRole);
    void toggleEnabled(Long id, Integer enabled, String currentRole);
    /** 刷新内存缓存（热更新） */
    void refreshCache();
}
