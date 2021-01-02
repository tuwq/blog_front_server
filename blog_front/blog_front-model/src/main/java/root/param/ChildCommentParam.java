package root.param;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;

@Data
public class ChildCommentParam {
	
	@NotNull(message="文章id不能为空")
	private Integer articleId;
	@NotNull(message="父评论id不能为空")
	private Integer parentId;
	@NotNull(message="根评论id不能为空")
	private Integer rootId;
	@NotBlank(message="文章内容不能为空")
	@Size(min=5,max=200,message="内容范围应该在5-100之间")
	private String content;
	
}
