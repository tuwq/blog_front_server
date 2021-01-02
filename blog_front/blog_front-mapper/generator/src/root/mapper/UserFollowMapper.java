package root.mapper;

import root.model.UserFollow;

public interface UserFollowMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(UserFollow record);

    int insertSelective(UserFollow record);

    UserFollow selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(UserFollow record);

    int updateByPrimaryKey(UserFollow record);
}