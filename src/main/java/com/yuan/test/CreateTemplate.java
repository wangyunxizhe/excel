package com.yuan.test;

import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.springframework.util.ResourceUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * Created by wangy on 2018/11/26.
 * <p>
 * 创建导入模板类
 */
public class CreateTemplate {

    public static void main(String[] args) throws Exception {
        //获取需要解析的xml文件
        File file = ResourceUtils.getFile("classpath:static/student.xml");
        SAXBuilder builder = new SAXBuilder();
        //开始解析xml文件
        Document parse = builder.build(file);
        //创建Excel
        HSSFWorkbook wb = new HSSFWorkbook();
        //创建sheet
        HSSFSheet sheet = wb.createSheet("Sheet0");
        //获取xml的根节点
        Element root = parse.getRootElement();
        //获取模板名称
        String templateName = root.getAttribute("name").getValue();
        int rownum = 0;//行号
        int column = 0;//列号
        //设置列宽
        Element colgroup = root.getChild("colgroup");
        setColumnWidth(sheet, colgroup);
        //设置标题行
        Element title = root.getChild("title");
        List<Element> trs = title.getChildren("tr");
        for (int i = 0; i < trs.size(); i++) {
            Element tr = trs.get(i);
            List<Element> tds = tr.getChildren("td");
            HSSFRow row = sheet.createRow(rownum);
            //创建样式
            HSSFCellStyle style = wb.createCellStyle();
            style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
            for (column = 0; column < tds.size(); column++) {
                Element td = tds.get(column);
                HSSFCell cell = row.createCell(column);
                Attribute rowSpan = td.getAttribute("rowspan");
                Attribute colSpan = td.getAttribute("colspan");
                Attribute value = td.getAttribute("value");
                if (value != null) {
                    String val = value.getValue();//得到td中value属性中的值
                    cell.setCellValue(val);//给单元格赋值
                    int rspan = rowSpan.getIntValue() - 1;//开始行，结束行
                    int cspan = colSpan.getIntValue() - 1;//结束列
                    //设置字体
                    HSSFFont font = wb.createFont();
                    font.setFontName("仿宋_GB2312");
                    font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);//字体加粗
                    font.setFontHeightInPoints((short) 12);
                    style.setFont(font);
                    cell.setCellStyle(style);//将设置好的样式，赋给单元格
                    //合并单元格
                    sheet.addMergedRegion(new CellRangeAddress(rspan, rspan, 0, cspan));
                }
            }
            rownum++;
        }
        //设置表头行
        Element thead = root.getChild("thead");
        trs = thead.getChildren("tr");
        for (int i = 0; i < trs.size(); i++) {
            Element tr = trs.get(i);
            HSSFRow row = sheet.createRow(rownum);
            List<Element> ths = tr.getChildren("th");
            for (column = 0; column < ths.size(); column++) {
                Element th = ths.get(column);
                Attribute valueAttr = th.getAttribute("value");
                HSSFCell cell = row.createCell(column);
                if (valueAttr != null) {
                    String vaule = valueAttr.getValue();
                    cell.setCellValue(vaule);
                }
            }
            rownum++;
        }
        //设置数据区域行
        Element tbody = root.getChild("tbody");
        Element tr = tbody.getChild("tr");
        int repeat = tr.getAttribute("repeat").getIntValue();
        List<Element> tds = tr.getChildren("td");
        for (int i = 0; i < repeat; i++) {
            HSSFRow row = sheet.createRow(rownum);
            for (column = 0; column < tds.size(); column++) {
                Element td = tds.get(column);
                HSSFCell cell = row.createCell(column);
                setType(wb, cell, td);
            }
            rownum++;
        }
        //生成Excel导入模板
        File tempFile = new File("e:/" + templateName + ".xls");
        tempFile.delete();
        tempFile.createNewFile();
        FileOutputStream out = FileUtils.openOutputStream(tempFile);
        wb.write(out);
        out.close();
    }

    /**
     * 设置列宽的方法
     */
    private static void setColumnWidth(HSSFSheet sheet, Element colgroup) {
        List<Element> cols = colgroup.getChildren("col");
        for (int i = 0; i < cols.size(); i++) {
            Element col = cols.get(i);
            Attribute width = col.getAttribute("width");
            String unit = width.getValue().replaceAll("[0-9,\\.]", "");//得到单位
            String value = width.getValue().replaceAll(unit, "");//得到不含单位的数值
            int v = 0;
            //如果定义的单位是px（像素）的话，进入if
            if (StringUtils.isBlank(unit) || "px".endsWith(unit)) {
                v = Math.round(Float.parseFloat(value) * 37F);//将宽度进行转换，以便确定excel中列的宽度
            } else if ("em".endsWith(unit)) {
                v = Math.round(Float.parseFloat(value) * 267.5F);
            }
            sheet.setColumnWidth(i, v);
        }
    }

    /**
     * 设置数据区域单元格样式
     */
    private static void setType(HSSFWorkbook wb, HSSFCell cell, Element td) {
        Attribute typeAttr = td.getAttribute("type");
        String type = typeAttr.getValue();
        HSSFDataFormat format = wb.createDataFormat();
        HSSFCellStyle style = wb.createCellStyle();
        if ("NUMERIC".equalsIgnoreCase(type)) {//判断td的type属性值是否是NUMERIC
            cell.setCellType(HSSFCell.CELL_TYPE_NUMERIC);//将单元格设置为数字类型
            Attribute formatAttr = td.getAttribute("format");
            String formatValue = formatAttr.getValue();
            formatValue = StringUtils.isNotBlank(formatValue) ? formatValue : "#,##0.00";
            style.setDataFormat(format.getFormat(formatValue));
        } else if ("STRING".equalsIgnoreCase(type)) {
            cell.setCellValue("");
            cell.setCellType(HSSFCell.CELL_TYPE_STRING);
            style.setDataFormat(format.getFormat("@"));
        } else if ("DATE".equalsIgnoreCase(type)) {
            cell.setCellType(HSSFCell.CELL_TYPE_NUMERIC);
            style.setDataFormat(format.getFormat("yyyy-m-d"));
        } else if ("ENUM".equalsIgnoreCase(type)) {//如果是枚举类型，那excel中应该表现为下拉框
            CellRangeAddressList regions = new CellRangeAddressList(cell.getRowIndex(), cell.getRowIndex()
                    , cell.getColumnIndex(), cell.getColumnIndex());
            Attribute enumAttr = td.getAttribute("format");
            String enumValue = enumAttr.getValue();
            //加载下拉列表内容
            DVConstraint constraint = DVConstraint.createExplicitListConstraint(enumValue.split(","));
            //加入数据有效性对象
            HSSFDataValidation dataValidation = new HSSFDataValidation(regions, constraint);
            wb.getSheetAt(0).addValidationData(dataValidation);
        }
        cell.setCellStyle(style);
    }

}
