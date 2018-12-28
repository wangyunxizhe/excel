package com.yuan.utils;

import com.yuan.entity.ColumnInfo;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangy on 2018/11/29.
 */
@Component
public class ColumnInfoUtil {

    /**
     * 动态获取表头
     */
    public List<ColumnInfo> getColumns(String templateId) throws Exception {
        List<ColumnInfo> list = new ArrayList<>();
        //获取需要解析的xml文件
        File file = ResourceUtils.getFile("classpath:static/" + templateId + ".xml");
        //解析模板文件
        SAXBuilder builder = new SAXBuilder();
        try {
            Document parse = builder.build(file);
            Element root = parse.getRootElement();
            Element thead = root.getChild("thead");
            Element tr = thead.getChild("tr");
            List<Element> children = tr.getChildren();
            ColumnInfo c = new ColumnInfo();
            //添加处理标志、失败代码，失败说明
            c = createColumnInfo("cgbz", "处理标志", 120, "center");
            list.add(c);
            c = createColumnInfo("hcode", "失败代码", 120, "center");
            list.add(c);
            c = createColumnInfo("msg", "失败说明", 120, "center");
            list.add(c);
            for (int i = 0; i < children.size(); i++) {
                Element th = children.get(i);
                String value = th.getAttribute("value").getValue();
                c = createColumnInfo("col" + i, value, 120, "center");
                list.add(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 创建column对象
     */
    private ColumnInfo createColumnInfo(String fieldId, String title, int width,
                                        String align) {
        ColumnInfo c = new ColumnInfo();
        c.setField(fieldId);
        c.setTitle(title);
        c.setWidth(width);
        c.setAlign(align);
        return c;
    }

}
