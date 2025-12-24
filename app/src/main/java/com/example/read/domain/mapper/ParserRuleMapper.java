package com.example.read.domain.mapper;

import com.example.read.data.entity.ParserRuleEntity;
import com.example.read.domain.model.ParserRule;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析规则实体与领域模型转换器
 */
public class ParserRuleMapper {

    /**
     * Entity 转 Domain
     */
    public static ParserRule toDomain(ParserRuleEntity entity) {
        if (entity == null) {
            return null;
        }
        
        ParserRule rule = new ParserRule();
        rule.setId(entity.getId());
        rule.setName(entity.getName());
        rule.setDomain(entity.getDomain());
        rule.setChapterListSelector(entity.getChapterListSelector());
        rule.setChapterTitleSelector(entity.getChapterTitleSelector());
        rule.setChapterLinkSelector(entity.getChapterLinkSelector());
        rule.setContentSelector(entity.getContentSelector());
        rule.setRemoveSelectorsFromString(entity.getRemoveSelectors());
        rule.setCreateTime(entity.getCreateTime());
        
        return rule;
    }

    /**
     * Domain 转 Entity
     */
    public static ParserRuleEntity toEntity(ParserRule rule) {
        if (rule == null) {
            return null;
        }
        
        ParserRuleEntity entity = new ParserRuleEntity(
            rule.getName(),
            rule.getDomain(),
            rule.getChapterListSelector(),
            rule.getChapterTitleSelector(),
            rule.getChapterLinkSelector(),
            rule.getContentSelector()
        );
        entity.setId(rule.getId());
        entity.setRemoveSelectors(rule.getRemoveSelectorsAsString());
        entity.setCreateTime(rule.getCreateTime());
        
        return entity;
    }

    /**
     * Entity 列表转 Domain 列表
     */
    public static List<ParserRule> toDomainList(List<ParserRuleEntity> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        
        List<ParserRule> rules = new ArrayList<>();
        for (ParserRuleEntity entity : entities) {
            rules.add(toDomain(entity));
        }
        return rules;
    }

    /**
     * Domain 列表转 Entity 列表
     */
    public static List<ParserRuleEntity> toEntityList(List<ParserRule> rules) {
        if (rules == null) {
            return new ArrayList<>();
        }
        
        List<ParserRuleEntity> entities = new ArrayList<>();
        for (ParserRule rule : rules) {
            entities.add(toEntity(rule));
        }
        return entities;
    }
}
