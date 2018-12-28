package com.yuan.controller;

import com.yuan.dao.ExportDao;
import com.yuan.entity.Student;
import com.yuan.utils.DB;
import com.yuan.utils.ExportUtil;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangy on 2018/11/30.
 */
@Controller
public class ExportController {

    @Autowired
    private ExportDao dao;

    @Autowired
    private ExportUtil exportUtil;

    @RequestMapping(value = "/tostudentlist", method = RequestMethod.GET)
    public String toImport() {
        return "exportList";
    }

    @RequestMapping(value = "/exportlist")
    @ResponseBody
    public JSONObject list(@RequestParam(value = "page", required = false) int page,
                           @RequestParam(value = "rows", required = false) int rows,
                           @RequestParam(value = "sort", required = false) String sort,
                           @RequestParam(value = "order", required = false) String order) {
        int currentPage = (page - 1) * rows;
        List<Student> stuList = dao.getStuList(currentPage, rows, sort, order);//学生列表
        int total = dao.getTotal();//学生列表总记录数
        JSONObject js = new JSONObject();
        js.put("total", total);
        js.put("rows", stuList);
        //反射时需要用的类
        js.put("className", ExportController.class.getName());
        //反射时需要用到的类方法
        js.put("methodName", "getStudents");
        //反射时需要用到的类方法参数
        js.put("invokepage", page);
        js.put("invokerows", rows);
        js.put("invokesort", sort);
        js.put("invokeorder", order);
        return js;
    }

    /**
     * 导出前台列表为excel文件
     */
    @RequestMapping(value = "/doexport")
    public void export(HttpServletResponse response,
                       @RequestParam(value = "className", required = false) String className,
                       @RequestParam(value = "methodName", required = false) String methodName,
                       @RequestParam(value = "fields", required = false) String fields,
                       @RequestParam(value = "titles", required = false) String titles,
                       @RequestParam(value = "page", required = false) int page,
                       @RequestParam(value = "rows", required = false) int rows,
                       @RequestParam(value = "sort", required = false) String sort,
                       @RequestParam(value = "order", required = false) String order) throws Exception {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=export.xls");
        //创建Excel
        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet sheet = wb.createSheet("sheet0");
        //获取类
        Class clazz = Class.forName(className);
        Object o = clazz.newInstance();
        Class[] types = {int.class, int.class, String.class, String.class};
        Object[] paras = {page, rows, sort, order};
        Method m = clazz.getDeclaredMethod(methodName, types);
        List list = (List) m.invoke(o, paras);
        exportUtil.outputHeaders(titles.split(","), sheet);
        exportUtil.outputColumns(fields.split(","), list, sheet, 1);
        //获取输出流，写入excel 并关闭
        ServletOutputStream out = response.getOutputStream();
        wb.write(out);
        out.flush();
        out.close();
    }

    /**
     * 专门用于反射时使用的方法
     */
    public List<Student> getStudents(int currentPage, int pageSize, String sort, String order) {
        Connection conn = DB.createConn();
        String sql = "select * from t_importstudent where 1=1 ";
        if (StringUtils.isNotBlank(sort)) {
            sql += "order by " + sort;
        }
        if (StringUtils.isNotBlank(order)) {
            sql += " " + order;
        }
        if (currentPage > 0 && pageSize > 0) {
            sql += " limit " + (currentPage - 1) * pageSize + " , " + pageSize;
        }
        PreparedStatement ps = DB.prepare(conn, sql);
        List<Student> slist = new ArrayList<Student>();
        try {
            ResultSet rs = ps.executeQuery();
            Student s = null;
            while (rs.next()) {
                s = new Student();
                s.setStunum(rs.getString("stunum"));
                s.setStuname(rs.getString("stuname"));
                s.setStuage(rs.getString("stuage"));
                s.setStusex(rs.getString("stusex"));
                s.setStubirthday(rs.getString("stubirthday"));
                s.setStuhobby(rs.getString("stuhobby"));

                slist.add(s);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        DB.close(ps);
        DB.close(conn);
        return slist;
    }

}
