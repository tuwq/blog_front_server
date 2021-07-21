package root.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import root.beans.JsonResult;
import root.beans.PageModel;
import root.beans.PageResult;
import root.configConstant.BlogConfigProperties;
import root.constant.RedisCode;
import root.constant.ResultCode;
import root.dto.ArticleDto;
import root.dto.ArticleCategoryDto;
import root.dto.ShowCategoryArticleDto;
import root.exception.CheckParamException;
import root.exception.NotFoundException;
import root.mapper.ArticleBindArticleCategoryMapper;
import root.mapper.ArticleMapper;
import root.mapper.ArticleCategoryMapper;
import root.model.Article;
import root.model.ArticleBindArticleCategory;
import root.model.ArticleCategory;
import root.param.PageParam;
import root.redis.RedisOperator;
import root.util.DtoUtil;
import root.util.JsonUtils;
import root.util.TimeAgoUtils;
import root.util.ValidatorUtil;

@Service
public class ArticleCategoryService {

	@Resource
	private ArticleCategoryMapper categoryMapper;
	@Resource
	private ArticleMapper articaleMapper;
	@Autowired
	private ArticleBindArticleCategoryMapper articleBindArticleCategoryMapper;
	@Resource
	private BlogConfigProperties blogConfigProperties;
	@Resource
	private RedisOperator redis;
	
	public PageResult<ArticleDto> categoryList(PageParam param, Integer id) {
		// 检查字段
		// 分类id的分类是否存在，不存在返回404
		// 分类id下的所有文章总数
		// 生成skip
		// 获得指定数量的文章和每篇文章的用户信息
		// 获得分类信息
		// 生成分页返回数据
		ValidatorUtil.check(param);
		if (id == null) {
			throw new CheckParamException("分类Id","为空");
		}
		int count = categoryMapper.countById(id);
		if (count == 0) {
			throw new NotFoundException(ResultCode.ITEM_NOT_FOUND,"分类不存在");
		}
		ArticleCategory category = categoryMapper.selectByPrimaryKey(id);
		Long total = articaleMapper.countAllByCategoryId(id);
		if(total == 0) {
			return PageResult.<ArticleDto>builder().data(Lists.newArrayList()).pageModel(new PageModel()).code(200).build();
		}
		param.buildSkip();
		List<Article> articleList = articaleMapper.categoryPage(param.getSkip(),param.getPageSize(),id);
		List<ArticleDto> articleDtoList = Lists.newArrayList();
		articleList.forEach(article -> {
			ArticleDto articleDto = DtoUtil.adapt(new ArticleDto(), article);
			articleDto.setTimeAgo(TimeAgoUtils.format(articleDto.getCreateTime()));
			articleDto.formatNoSecondTime();
			articleDtoList.add(articleDto);
		});
		PageModel pageModel = new PageModel(total,articleDtoList.size(),param.getCurrentPage(),param.getPageSize());
		return PageResult.<ArticleDto>builder().category(category).pageModel(pageModel).code(200).data(articleDtoList).build();
	}
	
	public JsonResult<List<ArticleDto>> praise(Integer quantity) {
		// 根据集体点赞数和更新时间获取指定数量文章数据
		if (quantity == null) {
			throw new CheckParamException("数量","未指定");
		}
		String cacheList = redis.get(RedisCode.ARTICLE_LIST_PRAISE_CACHE);
		if (cacheList != null) {
			return JsonResult.<List<ArticleDto>>success(JsonUtils.jsonToList(cacheList, ArticleDto.class));
		}
		List<Article> praiseList = articaleMapper.praiseByQuantity(quantity);
		List<ArticleDto> articaleDtoList = Lists.newArrayList();
		praiseList.forEach(articale -> {
			ArticleDto articaleDto = DtoUtil.adapt(new ArticleDto(), articale);
			articaleDto.setTimeAgo(TimeAgoUtils.format(articaleDto.getUpdateTime()));
			articaleDto.formatNoSecondTime();
			articaleDtoList.add(articaleDto);
		});
		redis.set(RedisCode.ARTICLE_LIST_PRAISE_CACHE, JsonUtils.objectToJson(articaleDtoList),blogConfigProperties.getCache().getArticleListIndexTimeout());
		return JsonResult.<List<ArticleDto>>success(articaleDtoList);
	}

	public JsonResult<ShowCategoryArticleDto> findListByCategoryIdAndQuantity(Integer articleCategoryId, Integer quantity) {
		// 根据集体点赞数和更新时间获得指定数量的分类文章的文章
		// 查找分类名称
		if (quantity == null) {
			throw new CheckParamException("数量","未指定");
		}
		String cacheDto = redis.get(RedisCode.ARTICLE_CATEGORY_CACHE+":"+ articleCategoryId);
		if (cacheDto != null) {
			return JsonResult.<ShowCategoryArticleDto>success(JsonUtils.jsonToPojo(cacheDto, ShowCategoryArticleDto.class));
		}
		List<ArticleBindArticleCategory> dbArticleBindArticleCategoryList = articleBindArticleCategoryMapper.findListByArticleCategoryIdWithLimitAndRandom(articleCategoryId, quantity);
		List<ArticleDto> articaleDtoList = Lists.newArrayList();
		for (int i = 0; i < dbArticleBindArticleCategoryList.size(); i++) {
			ArticleBindArticleCategory dbArticleBindArticleCategoryItem = dbArticleBindArticleCategoryList.get(i);
			Article dbArticle = articaleMapper.selectByPrimaryKey(dbArticleBindArticleCategoryItem.getArticleId());
			ArticleDto articaleDto = DtoUtil.adapt(new ArticleDto(), dbArticle);
			articaleDto.setTimeAgo(TimeAgoUtils.format(articaleDto.getUpdateTime()));
			articaleDto.formatNoSecondTime();
			articaleDtoList.add(articaleDto);
		}
		ArticleCategory category = categoryMapper.selectByPrimaryKey( articleCategoryId);
		if (category == null) { throw new CheckParamException("分类","不存在"); }
		ShowCategoryArticleDto showCADto = ShowCategoryArticleDto.builder().category(category).articaleList(articaleDtoList).build();
		redis.set(RedisCode.ARTICLE_CATEGORY_CACHE+":"+articleCategoryId, JsonUtils.objectToJson(showCADto),blogConfigProperties.getCache().getArticleListIndexTimeout());
		return JsonResult.<ShowCategoryArticleDto>success(showCADto);
	};

	public JsonResult<ShowCategoryArticleDto> hotDiscuss(Integer quantity) {
		if (quantity == null) {
			throw new CheckParamException("数量","未指定");
		}
		String cacheDto = redis.get(RedisCode.ARTICLE_LIST_COMMENT_CACHE);
		if (cacheDto != null) {
			return JsonResult.<ShowCategoryArticleDto>success(JsonUtils.jsonToPojo(cacheDto, ShowCategoryArticleDto.class));
		}
		List<Article> articaleList = articaleMapper.hotDiscuss(quantity);
		List<ArticleDto> articaleDtoList = Lists.newArrayList();
		articaleList.forEach(articale -> {
			ArticleDto articaleDto = DtoUtil.adapt(new ArticleDto(), articale);
			articaleDto.setTimeAgo(TimeAgoUtils.format(articaleDto.getUpdateTime()));
			articaleDto.formatNoSecondTime();
			articaleDtoList.add(articaleDto);
		});
		ArticleCategory category = new ArticleCategory();category.setName("热评文章");
		ShowCategoryArticleDto showCADto = ShowCategoryArticleDto.builder().category(category).articaleList(articaleDtoList).build();
		redis.set(RedisCode.ARTICLE_LIST_COMMENT_CACHE, JsonUtils.objectToJson(showCADto),blogConfigProperties.getCache().getArticleListIndexTimeout());
		return JsonResult.<ShowCategoryArticleDto>success(showCADto);
	}

	public JsonResult<List<ArticleDto>> weight(Integer quantity) {
		// 根据权重和更新时间
		if (quantity == null) {
			throw new CheckParamException("数量","未指定");
		}
		String cacheDtoList = redis.get(RedisCode.ARTICLE_LIST_WEIGHT_CACHE);
		if (cacheDtoList != null) {
			return JsonResult.<List<ArticleDto>>success(JsonUtils.jsonToList(cacheDtoList, ArticleDto.class));
		}
		List<Article> praiseList = articaleMapper.weightByQuantity(quantity);
		List<ArticleDto> articaleDtoList = Lists.newArrayList();
		praiseList.forEach(articale -> {
			ArticleDto articaleDto = DtoUtil.adapt(new ArticleDto(), articale);
			articaleDto.setTimeAgo(TimeAgoUtils.format(articaleDto.getUpdateTime()));
			articaleDto.formatNoSecondTime();
			articaleDtoList.add(articaleDto);
		});
		redis.set(RedisCode.ARTICLE_LIST_WEIGHT_CACHE, JsonUtils.objectToJson(articaleDtoList),blogConfigProperties.getCache().getArticleListIndexTimeout());
		return JsonResult.<List<ArticleDto>>success(articaleDtoList);
	}

	public JsonResult<List<ArticleDto>> newTime(Integer quantity) {
		// 根据创建时间
		if (quantity == null) {
			throw new CheckParamException("数量","未指定");
		}
		List<Article> praiseList = articaleMapper.createTimeByQuantity(quantity);
		List<ArticleDto> articaleDtoList = Lists.newArrayList();
		praiseList.forEach(articale -> {
			ArticleDto articaleDto = DtoUtil.adapt(new ArticleDto(), articale);
			articaleDto.setTimeAgo(TimeAgoUtils.format(articaleDto.getUpdateTime()));
			articaleDto.formatNoSecondTime();
			articaleDtoList.add(articaleDto);
		});
		return JsonResult.<List<ArticleDto>>success(articaleDtoList);
	}
	
	/**
	 * 所有分类的信息,包括子分类
	 * 所有分类的文章数量信息
	 * @return
	 */
	public JsonResult<List<ArticleCategoryDto>> all() {
		List<ArticleCategoryDto> data = new ArrayList<ArticleCategoryDto>();
		List<ArticleCategory> rootArticleCategoryList = categoryMapper.findByParentCategoryId(0);
		rootArticleCategoryList.stream().forEach( parentItem -> {
			ArticleCategoryDto parentItemDto = DtoUtil.adapt(new ArticleCategoryDto(), parentItem);
			Long articleCount = articaleMapper.countAllByCategoryId(parentItemDto.getId());
			parentItemDto.setArticleCount(articleCount);
			List<ArticleCategory> childCategoryList = categoryMapper.findByParentCategoryId(parentItemDto.getId());
			List<ArticleCategoryDto> childCategoryDtoList = childCategoryList
					.stream().map(child -> DtoUtil.adapt(new ArticleCategoryDto(), child)).collect(Collectors.toList());
			parentItemDto.setChildCategoryList(childCategoryDtoList);
			childCategoryDtoList.stream().forEach( childDto -> {
				Long articleCount2 = articaleMapper.countAllByCategoryId(childDto.getId());
				childDto.setArticleCount(articleCount2);
			});
			data.add(parentItemDto);
		});
		return JsonResult.<List<ArticleCategoryDto>>success(data);
	}

	/**
	 * 随机取出指定数量{@param quantity}文章
	 * @param articleCategoryId
	 * @param quantity
	 * @return
	 */
	public JsonResult<List<ArticleDto>> randomArticle(Integer articleCategoryId, Integer quantity) {
		List<ArticleBindArticleCategory> articleBindArticleCategoryList = articleBindArticleCategoryMapper.findListByArticleCategoryIdWithLimitAndRandom(articleCategoryId, quantity);
		List<ArticleDto> articaleDtoList = Lists.newArrayList();
		for (int i = 0; i < articleBindArticleCategoryList.size(); i++) {
			ArticleBindArticleCategory articleBindArticleCategoryItem = articleBindArticleCategoryList.get(i);
			Article dbArticle = articaleMapper.selectByPrimaryKey(articleBindArticleCategoryItem.getArticleId());
			ArticleDto articaleDto = DtoUtil.adapt(new ArticleDto(), dbArticle);
			articaleDto.setTimeAgo(TimeAgoUtils.format(articaleDto.getUpdateTime()));
			articaleDto.formatNoSecondTime();
			articaleDtoList.add(articaleDto);
		}
		return JsonResult.<List<ArticleDto>>success(articaleDtoList);
	}
}
