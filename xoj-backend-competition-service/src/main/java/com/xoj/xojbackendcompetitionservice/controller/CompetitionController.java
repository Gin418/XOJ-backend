package com.xoj.xojbackendcompetitionservice.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xoj.xojbackendcompetitionservice.service.CompetitionQuestionService;
import com.xoj.xojbackendcompetitionservice.service.CompetitionService;
import com.xoj.xojbackendcompetitionservice.service.UserCompetitionService;
import com.yupi.yuojbackendcommon.common.BaseResponse;
import com.yupi.yuojbackendcommon.common.ErrorCode;
import com.yupi.yuojbackendcommon.common.ResultUtils;
import com.yupi.yuojbackendcommon.constant.UserConstant;
import com.yupi.yuojbackendcommon.exception.BusinessException;
import com.yupi.yuojbackendcommon.exception.ThrowUtils;
import com.yupi.yuojbackendcommon.utils.GatewayHeaderUtil;
import com.yupi.yuojbackendmodel.model.dto.competition.CompetitionAddRequest;
import com.yupi.yuojbackendmodel.model.dto.competition.CompetitionJoinRequest;
import com.yupi.yuojbackendmodel.model.dto.competition.CompetitionQueryRequest;
import com.yupi.yuojbackendmodel.model.dto.competition.CompetitionUpdateRequest;
import com.yupi.yuojbackendmodel.model.entity.Competition;
import com.yupi.yuojbackendmodel.model.entity.CompetitionQuestion;
import com.yupi.yuojbackendmodel.model.entity.Question;
import com.yupi.yuojbackendmodel.model.vo.CompetitionQuestionDetailVO;
import com.yupi.yuojbackendmodel.model.vo.CompetitionQuestionSimpleVO;
import com.yupi.yuojbackendmodel.model.vo.CompetitionQuestionVO;
import com.yupi.yuojbackendmodel.model.vo.CompetitionRankItemVO;
import com.yupi.yuojbackendmodel.model.vo.CompetitionVO;
import com.yupi.yuojbackendmodel.model.vo.QuestionVO;
import com.yupi.yuojbackendserviceclient.service.QuestionFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/")
@Slf4j
public class CompetitionController {

    @Resource
    private CompetitionService competitionService;

    @Resource
    private UserCompetitionService userCompetitionService;

    @Resource
    private CompetitionQuestionService competitionQuestionService;

    @Resource
    private QuestionFeignClient questionFeignClient;

    /**
     * 参加竞赛
     *
     * @param competitionJoinRequest
     * @return
     */
    @PostMapping("/join")
    public BaseResponse<Long> joinCompetition(@RequestBody CompetitionJoinRequest competitionJoinRequest) {
        if (competitionJoinRequest == null || competitionJoinRequest.getCompetitionId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = GatewayHeaderUtil.getUserId();
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        log.info("joinCompetition request: {}", competitionJoinRequest);
        Long id = userCompetitionService.joinCompetition(competitionJoinRequest.getCompetitionId(), userId);
        return ResultUtils.success(id);
    }

    @PostMapping("/add")
    public BaseResponse<Long> addCompetition(@RequestBody CompetitionAddRequest competitionAddRequest) {
        if (competitionAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        log.info("addCompetition request: {}", competitionAddRequest);
        Competition competition = new Competition();
        BeanUtil.copyProperties(competitionAddRequest, competition);
        Long id = competitionService.addCompetition(competition, competitionAddRequest.getQuestionIds());
        return ResultUtils.success(id);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateCompetition(@RequestBody CompetitionUpdateRequest updateRequest) {
        if (updateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        log.info("updateCompetition request: {}", updateRequest);
        Competition competition = new Competition();
        BeanUtil.copyProperties(updateRequest, competition);
        boolean bool = competitionService.updateCompetition(competition, updateRequest.getQuestionIds());
        return ResultUtils.success(bool);
    }

    @GetMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteCompetition(@PathVariable Long id) {
        if (!UserConstant.ADMIN_ROLE.equals(GatewayHeaderUtil.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean bool = competitionService.deleteCompetition(id);
        return ResultUtils.success(bool);
    }

    @GetMapping("/{id}")
    public BaseResponse<CompetitionVO> getCompetition(@PathVariable Long id) {
        if (!UserConstant.ADMIN_ROLE.equals(GatewayHeaderUtil.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        Competition competition = competitionService.getById(id);
        ThrowUtils.throwIf(competition == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(CompetitionVO.objToVo(competition));
    }

    /**
     * 根据竞赛 id 查询题目列表（按题号顺序 A、B、C……返回，含竞赛内统计）
     *
     * @param competitionId 竞赛 id
     */
    @GetMapping("/questions/{competitionId}")
    public BaseResponse<List<CompetitionQuestionVO>> listCompetitionQuestions(@PathVariable Long competitionId) {
        if (competitionId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        log.info("listCompetitionQuestions request, competitionId = {}", competitionId);
        List<CompetitionQuestion> relationList = competitionQuestionService.listByCompetitionId(competitionId);
        List<CompetitionQuestionVO> questionVOList = relationList.stream().map(relation -> {
            CompetitionQuestionVO competitionQuestionVO = new CompetitionQuestionVO();
            BeanUtil.copyProperties(relation, competitionQuestionVO);
            competitionQuestionVO.setCustomId(competitionQuestionService.convertToQuestionNo(relation.getQuestionOrder()));
            Question question = questionFeignClient.getQuestionById(relation.getQuestionId());
            if (question != null) {
                competitionQuestionVO.setTitle(question.getTitle());
            }
            return competitionQuestionVO;
        }).collect(Collectors.toList());
        return ResultUtils.success(questionVOList);
    }

    /**
     * 根据竞赛 id 查询题目精简列表（仅题号、题目标题、题目 id）
     *
     * @param competitionId 竞赛 id
     */
    @GetMapping("/simpleQuestions/{competitionId}")
    public BaseResponse<List<CompetitionQuestionSimpleVO>> listCompetitionSimpleQuestions(@PathVariable Long competitionId) {
        if (competitionId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        log.info("listCompetitionSimpleQuestions request, competitionId = {}", competitionId);
        List<CompetitionQuestion> relationList = competitionQuestionService.listByCompetitionId(competitionId);
        List<CompetitionQuestionSimpleVO> voList = relationList.stream().map(relation -> {
            CompetitionQuestionSimpleVO simpleVO = new CompetitionQuestionSimpleVO();
            simpleVO.setQuestionId(relation.getQuestionId());
            String customId = competitionQuestionService.convertToQuestionNo(relation.getQuestionOrder());
            Question question = questionFeignClient.getQuestionById(relation.getQuestionId());
            if (question != null) {
                simpleVO.setLabel(customId + " " + question.getTitle());
            }
            return simpleVO;
        }).collect(Collectors.toList());
        return ResultUtils.success(voList);
    }

    /**
     * 通过竞赛 id 和题号（customId，如 A、B、C）查询题目详情
     *
     * @param competitionId 竞赛 id
     * @param customId      题号（A、B、C……或数字）
     */
    @GetMapping("/questions/{competitionId}/{customId}")
    public BaseResponse<CompetitionQuestionDetailVO> getCompetitionQuestionDetail(@PathVariable Long competitionId,
                                                                                  @PathVariable String customId) {
        if (competitionId == null || StringUtils.isBlank(customId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        log.info("getCompetitionQuestionDetail request, competitionId = {}, customId = {}", competitionId, customId);
        Integer questionOrder = competitionQuestionService.convertToQuestionOrder(customId);
        ThrowUtils.throwIf(questionOrder == null, ErrorCode.PARAMS_ERROR, "题号格式错误");
        CompetitionQuestion relation = competitionQuestionService.getByCompetitionIdAndOrder(competitionId, questionOrder);
        ThrowUtils.throwIf(relation == null, ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        Question question = questionFeignClient.getQuestionById(relation.getQuestionId());
        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        // 组装详情
        CompetitionQuestionDetailVO detailVO = new CompetitionQuestionDetailVO();
        BeanUtil.copyProperties(relation, detailVO);
        detailVO.setCustomId(competitionQuestionService.convertToQuestionNo(relation.getQuestionOrder()));
        QuestionVO questionVO = QuestionVO.objToVo(question);
        detailVO.setQuestionVO(questionVO);
        return ResultUtils.success(detailVO);
    }

    /**
     * 根据竞赛 id 查询排名（通过数、罚时、每题情况、用户信息）
     *
     * @param competitionId 竞赛 id
     */
    @GetMapping("/rank/{competitionId}")
    public BaseResponse<List<CompetitionRankItemVO>> getCompetitionRank(@PathVariable Long competitionId) {
        if (competitionId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        log.info("getCompetitionRank request, competitionId = {}", competitionId);
        return ResultUtils.success(competitionService.getRankList(competitionId));
    }

    /**
     * 题目顺序转题号：1→A，2→B……超过 26 道题时回退为数字
     */
    private String convertToQuestionNo(Integer questionOrder) {
        if (questionOrder == null || questionOrder < 1) {
            return "";
        }
        if (questionOrder <= 26) {
            return String.valueOf((char) ('A' + questionOrder - 1));
        }
        return String.valueOf(questionOrder);
    }

    /**
     * 题号转题目顺序：A→1，B→2……数字题号原样返回
     */
    private Integer convertToQuestionOrder(String customId) {
        String upper = customId.trim().toUpperCase();
        if (upper.length() == 1 && upper.charAt(0) >= 'A' && upper.charAt(0) <= 'Z') {
            return upper.charAt(0) - 'A' + 1;
        }
        try {
            int order = Integer.parseInt(upper);
            return order >= 1 ? order : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @PostMapping("/page")
    public BaseResponse<Page<Competition>> listCompetition(@RequestBody CompetitionQueryRequest competitionQueryRequest) {
        long current = competitionQueryRequest.getCurrent();
        long size = competitionQueryRequest.getPageSize();
        Page<Competition> result = competitionService.page(new Page<>(current, size), competitionService.queryWrapper(competitionQueryRequest));

        return ResultUtils.success(result);
    }
}
