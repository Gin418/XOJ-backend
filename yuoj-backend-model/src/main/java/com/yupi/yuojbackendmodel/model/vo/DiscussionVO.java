package com.yupi.yuojbackendmodel.model.vo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yupi.yuojbackendmodel.model.entity.Discussion;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 讨论视图
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class DiscussionVO implements Serializable {

    private final static Gson GSON = new Gson();

    /**
     * id
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Integer thumbNum;

    /**
     * 收藏数
     */
    private Integer favourNum;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 题目 id
     */
    private Long questionId;
    /**
     * 父评论 id（回复时非空，null 表示一级评论）
     */
    private Long parentId;
    /**
     * 被回复用户 id（回复展示“回复 @xxx”用）
     */
    private Long replyUserId;
    /**
     * 被回复的用户信息（回复展示“回复 @xxx”用）
     */
    private UserVO replyUser;
    /**
     * 回复数（仅一级评论返回，用于展示“查看回复”）
     */
    private Integer replyCount;
    /**
     * 第二级回复列表（仅主贴返回，最多最早的 2 条，按创建时间升序）
     */
    private List<DiscussionVO> replies;
    /**
     * 创建人信息
     */
    private UserVO user;

    /**
     * 是否已点赞
     */
    private Boolean hasThumb;

    /**
     * 是否已收藏
     */
    private Boolean hasFavour;

    /**
     * 包装类转对象
     *
     * @param discussionVO
     * @return
     */
    public static Discussion voToObj(DiscussionVO discussionVO) {
        if (discussionVO == null) {
            return null;
        }
        Discussion discussion = new Discussion();
        BeanUtils.copyProperties(discussionVO, discussion);
        return discussion;
    }

    /**
     * 对象转包装类
     *
     * @param discussion
     * @return
     */
    public static DiscussionVO objToVo(Discussion discussion) {
        if (discussion == null) {
            return null;
        }
        DiscussionVO discussionVO = new DiscussionVO();
        BeanUtils.copyProperties(discussion, discussionVO);
        return discussionVO;
    }
}
