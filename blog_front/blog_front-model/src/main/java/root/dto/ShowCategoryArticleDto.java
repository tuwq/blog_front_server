package root.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import root.model.ArticleCategory;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
public class ShowCategoryArticleDto {
	
	private List<ArticleDto> articaleList;
	
	private ArticleCategory category;
}
