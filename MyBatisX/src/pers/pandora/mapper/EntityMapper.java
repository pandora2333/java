package pers.pandora.mapper;

import com.sun.istack.internal.NotNull;
import pers.pandora.bean.Entity;

import java.util.List;

public interface EntityMapper {
    public Entity queryForOne(@NotNull int id);
    public Entity queryForEntity(@NotNull Entity entity);
    public void updateD(Entity entity);
    public List<Entity> queryForList(int id,int no);
    public void insert(String dname,String db_source);
    public void delete(int id1,int id2);
    public void update(String name,int id);
}
