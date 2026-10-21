package com.yupi.yuojbackendmodel.model.dto.competition;

import com.yupi.yuojbackendcommon.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 竞赛提交记录查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CompetitionSubmitQueryRequest extends PageRequest implements Serializable {

    /**
     * 竞赛 id（必填）
     */
    private Long competitionId;

    /**
     * 题目id
     */
    private Long questionId;

    /**
     * 编程语言
     */
    private Integer languageId;

    /**
     * 测评状态
     */
    private String status;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 是否只查询自己的提交
     */
    private Boolean onlyMine = false;

    private static final long serialVersionUID = 1L;
}
