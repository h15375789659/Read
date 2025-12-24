package com.example.read.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.read.data.entity.ParserRuleEntity;

import java.util.List;

/**
 * 解析规则数据访问对象 - 提供解析规则表的CRUD操作
 */
@Dao
public interface ParserRuleDao {

    @Query("SELECT * FROM parser_rules ORDER BY createTime DESC")
    LiveData<List<ParserRuleEntity>> getAllRules();

    @Query("SELECT * FROM parser_rules")
    List<ParserRuleEntity> getAllRulesSync();

    @Query("SELECT * FROM parser_rules WHERE id = :ruleId")
    ParserRuleEntity getRuleById(long ruleId);

    @Query("SELECT * FROM parser_rules WHERE domain = :domain")
    ParserRuleEntity getRuleByDomain(String domain);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertRule(ParserRuleEntity rule);

    @Update
    void updateRule(ParserRuleEntity rule);

    @Delete
    void deleteRule(ParserRuleEntity rule);

    @Query("DELETE FROM parser_rules WHERE id = :ruleId")
    void deleteRuleById(long ruleId);

    @Query("SELECT COUNT(*) FROM parser_rules")
    int getRuleCount();
}
