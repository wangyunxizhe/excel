package com.yuan.controller;

import com.yuan.dao.ImportDataDao;
import com.yuan.entity.ColumnInfo;
import com.yuan.entity.ImportData;
import com.yuan.entity.ImportDataDetail;
import com.yuan.entity.Template;
import com.yuan.utils.ColumnInfoUtil;
import com.yuan.utils.CreateTemplateUtil;
import com.yuan.utils.ImportCheckUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by wangy on 2018/11/28.
 * <p>
 * 用360浏览器打开，其他浏览器均无法使用上传进度条插件
 */
@Controller
public class ImportDataController {

    @Autowired
    private ImportDataDao dao;

    @Autowired
    private CreateTemplateUtil util;

    @Autowired
    private ImportCheckUtil cUtil;

    @Autowired
    private ColumnInfoUtil infoUtil;

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public String index() {
        return "layout";
    }

    @RequestMapping(value = "/toimport", method = RequestMethod.GET)
    public String toImport() {
        return "importList";
    }

    /**
     * easyUI初始化查询“数据导入”列表方法
     */
    @RequestMapping(value = "/list")
    @ResponseBody
    public JSONObject list(@RequestParam(value = "page", required = false) int page,
                           @RequestParam(value = "rows", required = false) int rows,
                           @RequestParam(value = "sort", required = false) String sort,
                           @RequestParam(value = "order", required = false) String order) {
        int currentPage = (page - 1) * rows;
        List<ImportData> importDataList = dao.list(currentPage, rows, sort, order);
        JSONObject js = new JSONObject();
        js.put("total", importDataList.size());
        js.put("rows", importDataList);
        return js;
    }

    /**
     * 加载弹窗中选择模板下拉框中的数据
     */
    @RequestMapping(value = "/gettemplates")
    @ResponseBody
    public JSONArray getTemplates() {
        List<Template> list = new ArrayList<>();
        //加入工程中的student.xml的文件名
        Template t = new Template();
        t.setTemplateId("student");
        t.setTemplateName("student");
        list.add(t);
        return JSONArray.fromObject(list);
    }

    /**
     * 下载导入模板的方法
     */
    @RequestMapping(value = "/download")
    public void download(HttpServletResponse response,
                         @RequestParam(value = "templateId", required = false) String templateId) throws Exception {
        util.createTemplate(templateId, response);
    }

    /**
     * 文件上传处理方法
     */
    @RequestMapping(value = "/doupload")
    @ResponseBody
    public JSONObject upload(@RequestParam(value = "fileInput", required = false) MultipartFile fileInput,
                             @RequestParam(value = "templateId", required = false) String templateId) {
        JSONObject js = new JSONObject();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String dateNow = df.format(new Date());
        //保存主表信息
        ImportData importData = new ImportData();
        importData.setImportid(String.valueOf(System.currentTimeMillis()));
        importData.setImportDataType(templateId);
        importData.setImportDate(dateNow);
        importData.setImportStatus("1");//导入成功
        importData.setHandleDate(null);
        importData.setHandleStatus("0");//未处理
        dao.saveImportData(importData);
        try {
            //读取Excel文件
            HSSFWorkbook wb = new HSSFWorkbook(fileInput.getInputStream());
            HSSFSheet sheet = wb.getSheetAt(0);
            //获取需要解析的xml文件
            File file = ResourceUtils.getFile("classpath:static/" + templateId + ".xml");
            //解析xml模板文件
            SAXBuilder builder = new SAXBuilder();
            Document parse = builder.build(file);
            Element root = parse.getRootElement();
            Element tbody = root.getChild("tbody");
            Element tr = tbody.getChild("tr");
            List<Element> children = tr.getChildren("td");
            //解析excel开始行，开始列
            int firstRow = tr.getAttribute("firstrow").getIntValue();
            int firstCol = tr.getAttribute("firstcol").getIntValue();
            //获取excel最后一行行号
            int lastRowNum = sheet.getLastRowNum();
            //循环每一行处理数据
            for (int i = firstRow; i <= lastRowNum; i++) {
                //初始化明细数据
                ImportDataDetail importDataDetail = new ImportDataDetail();
                importDataDetail.setImportId(importData.getImportid());
                importDataDetail.setCgbz("0");//未处理
                //读取某行
                HSSFRow row = sheet.getRow(i);
                //判断该行是否为空
                if (cUtil.isEmptyRow(row)) {
                    continue;
                }
                int lastCellNum = row.getLastCellNum();
                //如果非空行，则取所有单元格的值
                for (int j = firstCol; j < lastCellNum; j++) {
                    Element td = children.get(j - firstCol);
                    HSSFCell cell = row.getCell(j);
                    //如果单元格为null,继续处理下一个cell
                    if (cell == null) {
                        continue;
                    }
                    //获取单元格属性值
                    String value = cUtil.getCellValue(cell, td);
                    //导入明细实体赋值
                    if (StringUtils.isNotBlank(value)) {
                        if (value.indexOf("#000") >= 0) {
                            String[] info = value.split(",");
                            importDataDetail.setHcode(info[0]);
                            importDataDetail.setMsg(info[1]);
                            BeanUtils.setProperty(importDataDetail, "col" + j, info[2]);
                        } else {
                            BeanUtils.setProperty(importDataDetail, "col" + j, value);
                        }
                    }
                }
                dao.saveImportDataDetail(importDataDetail);
            }
            js.put("status", "ok");
            js.put("message", "导入成功！");
        } catch (Exception e) {
            js.put("status", "fail");
            js.put("message", "导入失败！");
            e.printStackTrace();
        }
        return js;
    }

    /**
     * 动态获取表头信息
     */
    @RequestMapping(value = "/columns")
    @ResponseBody
    public JSONArray columns(@RequestParam(value = "templateId", required = false) String templateId)
            throws Exception {
        //获取表头信息
        List<ColumnInfo> list = infoUtil.getColumns(templateId);
        return JSONArray.fromObject(list);
    }

    /**
     * 获取明细数据
     */
    @RequestMapping(value = "/columndatas")
    @ResponseBody
    public JSONObject columndatas(@RequestParam(value = "importDataId", required = false) String importDataId) {
        JSONObject js = new JSONObject();
        //获取明细数据
        List<ImportDataDetail> list = dao.getImportDataDetailsByMainId(importDataId);
        js.put("total", list.size());
        js.put("rows", list);
        return js;
    }

    /**
     * 确认导入
     */
    @RequestMapping(value = "/doimport")
    @ResponseBody
    public JSONObject doimport(@RequestParam(value = "importDataId", required = false) String importDataId) {
        JSONObject js = new JSONObject();
        //将导入的明细数据已到student表中
        dao.saveStudents(importDataId);
        //修改主表、明细表处理标志及时间
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateNow = sf.format(new Date());
        dao.updImportDataStatus(dateNow, importDataId);
        dao.updImportDataDetailStatus(importDataId);
        js.put("status", "ok");
        js.put("message", "确认成功！");
        return js;
    }

}
