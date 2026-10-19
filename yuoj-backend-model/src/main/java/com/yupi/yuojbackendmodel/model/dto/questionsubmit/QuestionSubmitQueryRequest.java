package com.yupi.yuojbackendmodel.model.dto.questionsubmit;

import com.yupi.yuojbackendcommon.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 查询请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuestionSubmitQueryRequest extends PageRequest implements Serializable {

    /**
     * 编程语言
     */
    private Integer languageId;

    /**
     * 题号
     */
    private String customId;

    /**
     * 题目id
     */
    private List<Long> questionIds;

    /**
     * 测评状态
     */
    private String status;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 用户 id 列表（按昵称查询时由服务端填充）
     */
    private List<Long> userIds;

    /**
     * 用户昵称（模糊查询，需先转为用户 id 列表）
     */
    private String userName;

    /**
     * 是否只查询自己的
     */
    private Boolean onlyMine = false;

    private static final long serialVersionUID = 1L;
}
