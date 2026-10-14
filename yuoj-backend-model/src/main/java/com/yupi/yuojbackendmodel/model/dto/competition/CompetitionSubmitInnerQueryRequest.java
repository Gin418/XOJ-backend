package com.yupi.yuojbackendmodel.model.dto.competition;

import com.yupi.yuojbackendcommon.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 竞赛提交记录内部查询请求（竞赛服务 → 题目服务）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CompetitionSubmitInnerQueryRequest extends PageRequest implements Serializable {

    /**
     * 竞赛 id
     */
    private Long competitionId;

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 测评状态
     */
    private String status;

    /**
     * 用户 id 列表
     */
    private List<Long> userIds;

    private static final long serialVersionUID = 1L;
}
