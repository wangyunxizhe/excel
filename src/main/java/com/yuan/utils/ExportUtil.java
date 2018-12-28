package com.yuan.utils;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

@Component
public class ExportUtil {

    /**
     * 设置sheet表头信息
     */
    public void outputHeaders(String[] headersInfo, HSSFSheet sheet) {
        HSSFRow row = sheet.createRow(0);
        for (int i = 0; i < headersInfo.length; i++) {
            sheet.setColumnWidth(i, 4000);
            row.createCell(i).setCellValue(headersInfo[i]);
        }
    }

    public void outputColumns(String[] headersInfo,
                              List columnsInfo, HSSFSheet sheet, int rowIndex) {
        HSSFRow row;
        int headerSize = headersInfo.length;//表头长度
        int columnSize = columnsInfo.size();//总记录数
        //循环插入多少行
        for (int i = 0; i < columnSize; i++) {
            row = sheet.createRow(rowIndex + i);
            Object obj = columnsInfo.get(i);
            //循环每行多少列
            for (int j = 0; j < headerSize; j++) {
                Object value = getFieldValueByName(headersInfo[j], obj);
                row.createCell(j).setCellValue(value.toString());
            }
        }
    }

    /**
     * 根据对象的属性获取值
     */
    private Object getFieldValueByName(String fieldName, Object obj) {
        String firstLetter = fieldName.substring(0, 1).toUpperCase();
        String getter = "get" + firstLetter + fieldName.substring(1);
        try {
            Method method = obj.getClass().getMethod(getter, new Class[]{});
            Object value = method.invoke(obj, new Object[]{});
            return value;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("属性不存在！");
            return null;
        }
    }
}
