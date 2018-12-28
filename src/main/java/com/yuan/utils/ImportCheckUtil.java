package com.yuan.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.jdom.Element;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by wangy on 2018/11/29.
 */
@Component
public class ImportCheckUtil {

    /**
     * 判断某行是否为空
     */
    public boolean isEmptyRow(HSSFRow row) {
        boolean flag = true;
        for (int i = 0; i < row.getLastCellNum(); i++) {
            HSSFCell cell = row.getCell(i);
            if (cell != null) {
                if (StringUtils.isNotBlank(cell.toString())) {
                    return false;
                }
            }
        }
        return flag;
    }

    /**
     * 获取单元格值，并且进行校验
     */
    public String getCellValue(HSSFCell cell, Element td) {
        //首先获取单元格位置
        int i = cell.getRowIndex() + 1;
        int j = cell.getColumnIndex() + 1;
        String returnValue = "";//返回值
        try {
            //获取模板文件对单元格格式限制
            String type = td.getAttribute("type").getValue();
            boolean isNullAble = td.getAttribute("isnullable").getBooleanValue();
            int maxlength = 9999;
            if (td.getAttribute("maxlength") != null) {
                maxlength = td.getAttribute("maxlength").getIntValue();
            }
            String value = null;
            //根据格式取出单元格的值
            switch (cell.getCellType()) {
                case HSSFCell.CELL_TYPE_STRING: {
                    value = cell.getStringCellValue();
                    break;
                }
                case HSSFCell.CELL_TYPE_NUMERIC: {
                    if ("datetime,date".indexOf(type) >= 0) {
                        Date date = cell.getDateCellValue();
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                        value = df.format(date);
                    } else {
                        double numericCellValue = cell.getNumericCellValue();
                        value = String.valueOf(numericCellValue);
                    }
                    break;
                }
            }
            //对非空、长度进行校验
            if (!isNullAble && StringUtils.isBlank(value)) {
                //错误编码,错误位置原因,单位格的值
                returnValue = "#0001,第" + i + "行第" + j + "列不能为空！,excel中的值为：" + value;
            } else if (StringUtils.isNotBlank(value) && (value.length() > maxlength)) {
                returnValue = "#0002,第" + i + "行第" + j + "列长度超过最大长度！,excel中的值为：" + value;
            } else {
                returnValue = value;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnValue;
    }

}
