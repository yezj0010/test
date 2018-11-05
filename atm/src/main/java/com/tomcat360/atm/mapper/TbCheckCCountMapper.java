package com.tomcat360.atm.mapper;

import com.tomcat360.atm.model.TbCheckCCount;
import com.tomcat360.atm.model.TbCheckCCountExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TbCheckCCountMapper {
    int countByExample(TbCheckCCountExample example);

    int deleteByExample(TbCheckCCountExample example);

    int deleteByPrimaryKey(Long id);

    int insert(TbCheckCCount record);

    int insertSelective(TbCheckCCount record);

    List<TbCheckCCount> selectByExample(TbCheckCCountExample example);

    TbCheckCCount selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") TbCheckCCount record, @Param("example") TbCheckCCountExample example);

    int updateByExample(@Param("record") TbCheckCCount record, @Param("example") TbCheckCCountExample example);

    int updateByPrimaryKeySelective(TbCheckCCount record);

    int updateByPrimaryKey(TbCheckCCount record);
}