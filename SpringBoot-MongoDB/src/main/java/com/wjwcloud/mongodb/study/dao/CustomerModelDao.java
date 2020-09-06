package com.wjwcloud.mongodb.study.dao;

import java.util.Map;


public interface CustomerModelDao {

    /**
     * 自定义接口实现查询
     * @param pageNo
     * @param pageSize
     * @param name
     * @return
     */
    Map<String, Object> findPageBySearch(int pageNo, int pageSize, String name);
}
