package com.example.demo1.Mapper;
import com.example.demo1.Entity.food;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface foodMapper {
    @Select("SELECT name FROM food")
    List<String> findAllNames();

    @Select("SELECT * FROM food WHERE name = #{name}")
    food findByName(String name);

    @Select("SELECT * FROM food")
    List<food> findAll();
}

