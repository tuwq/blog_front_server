package root.mapper;


import org.apache.ibatis.annotations.Param;
import root.model.ArticleBindArticleCategory;

import java.util.List;

public interface ArticleBindArticleCategoryMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(ArticleBindArticleCategory record);

    int insertSelective(ArticleBindArticleCategory record);

    ArticleBindArticleCategory selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(ArticleBindArticleCategory record);

    int updateByPrimaryKey(ArticleBindArticleCategory record);

    /**
     * 获取某文章分类获取关联记录
     * 根据quantity
     * 随机获取
     * @param articleCategoryId
     * @param quantity
     * @return
     */
    List<ArticleBindArticleCategory> findListByArticleCategoryIdWithLimitAndRandom(@Param("articleCategoryId") Integer articleCategoryId,
                                                                                   @Param("quantity") Integer quantity);
}