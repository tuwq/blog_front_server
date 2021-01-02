package root.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import root.async.IncrDataHandler;
import root.beans.JsonResult;
import root.configConstant.BlogConfigProperties;
import root.constant.RedisCode;
import root.constant.ResultCode;
import root.dto.ArticleDto;
import root.exception.CheckParamException;
import root.exception.NotFoundException;
import root.mapper.ArticleMapper;
import root.mapper.ArticleCategoryMapper;
import root.model.Article;
import root.model.ArticleCategory;
import root.redis.RedisOperator;
import root.util.DtoUtil;
import root.util.JsonUtils;
import root.util.TimeAgoUtils;

@Service
public class ArticaleService {

	@Resource
	private IncrDataHandler incrDataHandler;
	@Resource
	private ArticleMapper articaleMapper;
	@Resource
	private ArticleCategoryMapper categoryMapper;
	@Resource
	private RedisOperator redis;
	@Resource
	private BlogConfigProperties blogConfigProperties;
	
	public JsonResult<ArticleDto> detail(Integer id) {
		// 检查字段,访问id不存在去404
		// 检查资源是否存在,不存在去404
		// 获得文章信息
		// 获得文章分类信息
		// 获得文章用户信息
		// 获得上一篇和下一篇的文章数据
		if (id == null) {
			throw new NotFoundException(ResultCode.ITEM_NOT_FOUND,"访问文章id为空");
		}
		int count = articaleMapper.countById(id);
		if (count == 0) {
			throw new NotFoundException(ResultCode.ITEM_NOT_FOUND,"访问文章的不存在");
		}
		String artDtoJson = redis.get(RedisCode.ARTICLE_INFO_CACHE+":"+id);
		if (artDtoJson != null) {
			ArticleDto cacheDto = JsonUtils.jsonToPojo(artDtoJson, ArticleDto.class);
			incrDataHandler.articleBrowseIncr(id);
			return JsonResult.<ArticleDto>success(cacheDto);
		}
		Article articale = articaleMapper.getByIdWithUser(id);
		List<ArticleCategory> categoryList = categoryMapper.getArtCategoryListById(id);
		articale.setArticleCategoryList(categoryList);
		ArticleDto articaleDto = DtoUtil.adapt(new ArticleDto(), articale);
		Article prev = articaleMapper.getPrev(id);
		Article next = articaleMapper.getNext(id);
		articaleDto.setPrev(prev);
		articaleDto.setNext(next);
		articaleDto.setTimeAgo(TimeAgoUtils.format(articaleDto.getUpdateTime()));
		articaleDto.formatNoSecondTime();
		redis.set(RedisCode.ARTICLE_INFO_CACHE+":"+id,JsonUtils.objectToJson(articaleDto),blogConfigProperties.getCache().getArticleInfoTimeout());
		incrDataHandler.articleBrowseIncr(id);
		return JsonResult.<ArticleDto>success(articaleDto);
	}
}
