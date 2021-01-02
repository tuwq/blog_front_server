package root.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import root.beans.PageModel;
import root.beans.PageResult;
import root.constant.ResultCode;
import root.dto.ArticleDto;
import root.exception.CheckParamException;
import root.exception.SearchNoResultException;
import root.mapper.ArticleMapper;
import root.mapper.ArticleCategoryMapper;
import root.model.Article;
import root.model.ArticleCategory;
import root.param.PageParam;
import root.util.DtoUtil;
import root.util.TimeAgoUtils;
import root.util.ValidatorUtil;

@Service
public class SearchService {

	@Resource
	private ArticleMapper articaleMapper;
	@Resource
	private ArticleCategoryMapper categoryMapper;
	
	public PageResult<ArticleDto> pageKeyword(PageParam param, String keyword) {
		// 检查字段
		// 获得根据关键字查询的的文章总数
		// 如果没有数量返回无内容异常
		// 生成skip
		// 获得根据关键字查询的分页数据
		// 获得这些文章的分类信息,用户信息
		// 生成分页返回数据
		ValidatorUtil.check(param);
		if (StringUtils.isBlank(keyword)) {
			throw new CheckParamException("关键字","不能为空");
		}
		Long total = articaleMapper.countByKeyword(keyword);
		if (total == 0) {
			throw new SearchNoResultException(ResultCode.SEARCH_KEYWORD_NOT_RESULT,"没有找到符合条件的搜索结果");
		}
		param.buildSkip();
		List<Article> data = articaleMapper.pageByKeyWord(keyword,param.getSkip(),param.getPageSize());
		List<Integer> ids = data.stream().map( item -> item.getId()).collect(Collectors.toList());
		for (int i = 0; i< ids.size();i++ ) {
			List<ArticleCategory> cateList = categoryMapper.getArtCategoryListById(ids.get(i));
			data.get(i).setArticleCategoryList(cateList);
		}
		List<ArticleDto> dtoList = data.stream().map(item -> DtoUtil.adapt(new ArticleDto(), item)).collect(Collectors.toList());
		dtoList.forEach(dto -> {
			List<String> cateNameList = dto.getArticleCategoryList().stream().map(item -> item.getName()).collect(Collectors.toList());
			dto.setArticleCategoryName(String.join(",", cateNameList));
			dto.setOperatorerName(dto.getUser().getUsername());
			dto.setTimeAgo(TimeAgoUtils.format(dto.getUpdateTime()));
			dto.formatNoSecondTime();
		});
		PageModel pageModel = new PageModel(total,dtoList.size(),param.getCurrentPage(),param.getPageSize());
		return PageResult.<ArticleDto>builder().pageModel(pageModel).data(dtoList).code(200).build();
	}

	public PageResult<ArticleDto> pageAll(PageParam param) {
		// 检查字段
		// 查询文章总数
		// 没有数量返回无内容异常
		// 生成skip
		// 获得文章分页数据
		// 获得这些文章的用户信息和分类信息
		// 生成分页数据返回
		ValidatorUtil.check(param);
		Long total = articaleMapper.countAll();
		if (total == 0) {
			throw new SearchNoResultException(ResultCode.SEARCH_KEYWORD_NOT_RESULT,"没有找到任何文章");
		}
		param.buildSkip();
		List<Article> data = articaleMapper.pageWithUser(param.getSkip(),param.getPageSize());
		List<Integer> ids = data.stream().map(item -> item.getId()).collect(Collectors.toList());
		for (int i = 0; i< ids.size();i++ ) {
			List<ArticleCategory> cateList = categoryMapper.getArtCategoryListById(ids.get(i));
			data.get(i).setArticleCategoryList(cateList);
		}
		List<ArticleDto> dtoList = data.stream().map(item -> DtoUtil.adapt(new ArticleDto(), item)).collect(Collectors.toList());
		dtoList.forEach(dto -> {
			List<String> cateNameList = dto.getArticleCategoryList().stream().map(item -> item.getName()).collect(Collectors.toList());
			dto.setArticleCategoryName(String.join(",", cateNameList));
			dto.setOperatorerName(dto.getUser().getUsername());
			dto.setTimeAgo(TimeAgoUtils.format(dto.getUpdateTime()));
			dto.formatNoSecondTime();
		});
		PageModel pageModel = new PageModel(total,dtoList.size(),param.getCurrentPage(),param.getPageSize());
		return PageResult.<ArticleDto>builder().pageModel(pageModel).data(dtoList).code(200).build();
	}

	
}
