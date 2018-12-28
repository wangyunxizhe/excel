package com.yuan.dao;

import com.yuan.entity.Student;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Created by wangy on 2018/11/30.
 */
public interface ExportDao {

    /**
     * 查询学生列表信息
     */
    List<Student> getStuList(@Param("currentPage") int currentPage, @Param("pageSize") int pageSize,
                             @Param("sort") String sort, @Param("order") String order);

    int getTotal();
}
