/**
 * Copyright (c) LiveV Technologies(China),Inc.
 *
 * @Package: com.snc.vo
 *
 * @FileName: InstancePropertiesModel.java
 *
 * @Description: TODO(用一句话描述该文件做什么)
 *
 * @author: LC
 *
 * @date 2016年8月24日-下午4:04:33
 *
 * @version 1.0.0
 */
package com.snc.vo;

import java.util.Properties;

/**
 * @ClassName: InstancePropertiesModel
 *
 * @Description: TODO(这里用一句话描述这个类的作用)
 */
/**
 * 
 * @ClassName: InstancePropertiesModel
 *
 * @Description: TODO(这里用一句话描述这个类的作用)
 */
public class InstancePropertiesModel {
    public static String COUNT_STR = "count";
    public static String ITEM_NAME_STR = "item.0.name";
    public static String ITEM_LOC_STR = "item.0.loc";
    public static String ITEM_PID = "item.0.pid";
    public static String ITEM_ROOT = "item.0.root";

    private String count;
    private String name;
    private String loc;
    private String pid;
    private String root;

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLoc() {
        return loc;
    }

    public void setLoc(String loc) {
        this.loc = loc;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    /**
     * 
     * @Title: transformProperties
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @param properties
     * @return 参数
     * @return InstancePropertiesModel 返回类型
     * @throws
     */
    public static InstancePropertiesModel transformProperties(Properties properties) {
        try {
            if (properties == null) {
                return null;
            }

            InstancePropertiesModel model = new InstancePropertiesModel();

            String count = properties.getProperty(InstancePropertiesModel.COUNT_STR);
            String loc = properties.getProperty(InstancePropertiesModel.ITEM_LOC_STR);
            String name = properties.getProperty(InstancePropertiesModel.ITEM_NAME_STR);
            String pid = properties.getProperty(InstancePropertiesModel.ITEM_PID);
            String root = properties.getProperty(InstancePropertiesModel.ITEM_ROOT);

            model.setCount(count);
            model.setLoc(loc);
            model.setName(name);
            model.setPid(pid);
            model.setRoot(root);

            return model;
        } catch (Exception e) {
            return null;
        }
    }
}
