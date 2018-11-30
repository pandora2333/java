package pers.pandora.mapper;

import com.sun.istack.internal.NotNull;
import pers.pandora.bean.User;

public interface UserMapper {
    public User queryForUser(@NotNull User user);
    public void updateD(User user);
}
