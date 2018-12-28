package com.yuan.dao;

import com.yuan.entity.ImportData;
import com.yuan.entity.ImportDataDetail;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Created by wangy on 2018/11/28.
 */
public interface ImportDataDao {

    /**
     * 查询导入列表信息
     */
    List<ImportData> list(@Param("currentPage") int currentPage, @Param("pageSize") int pageSize,
                          @Param("sort") String sort, @Param("order") String order);

    /**
     * 保存主表信息
     */
    void saveImportData(ImportData importData);

    /**
     * 保存明细表数据
     */
    void saveImportDataDetail(ImportDataDetail importDataDetail);

    /**
     * 根据主表id获取明细信息
     */
    List<ImportDataDetail> getImportDataDetailsByMainId(@Param("importDataId") String importDataId);

    /**
     * 保存student
     */
    void saveStudents(@Param("importDataId") String importDataId);

    /**
     * 更新主表处理标志
     */
    void updImportDataStatus(@Param("dateNow") String dateNow, @Param("importDataId") String importDataId);

    /**
     * 更新明细表处理标志
     */
    void updImportDataDetailStatus(@Param("importDataId") String importDataId);
}
