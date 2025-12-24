package com.example.read.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.read.data.entity.CategoryEntity;

import java.util.List;

/**
 * 分类数据访问对象 - 提供分类表的CRUD操作
 */
@Dao
public interface CategoryDao {
    
    /**
     * 获取所有分类，按排序顺序排列
     */
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, createdTime ASC")
    LiveData<List<CategoryEntity>> getAllCategories();
    
    /**
     * 同步获取所有分类
     */
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, createdTime ASC")
    List<CategoryEntity> getAllCategoriesSync();
    
    /**
     * 获取所有分类名称
     */
    @Query("SELECT name FROM categories ORDER BY sortOrder ASC, createdTime ASC")
    LiveData<List<String>> getAllCategoryNames();
    
    /**
     * 根据名称获取分类
     */
    @Query("SELECT * FROM categories WHERE name = :name")
    CategoryEntity getCategoryByName(String name);
    
    /**
     * 检查分类是否存在
     */
    @Query("SELECT COUNT(*) FROM categories WHERE name = :name")
    int categoryExists(String name);
    
    /**
     * 插入分类
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertCategory(CategoryEntity category);
    
    /**
     * 批量插入分类
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertCategories(List<CategoryEntity> categories);
    
    /**
     * 删除分类
     */
    @Delete
    void deleteCategory(CategoryEntity category);
    
    /**
     * 根据名称删除分类
     */
    @Query("DELETE FROM categories WHERE name = :name")
    void deleteCategoryByName(String name);
    
    /**
     * 获取分类数量
     */
    @Query("SELECT COUNT(*) FROM categories")
    int getCategoryCount();
    
    /**
     * 获取最大排序顺序
     */
    @Query("SELECT MAX(sortOrder) FROM categories")
    Integer getMaxSortOrder();
}
