package com.xoj.xojbackendcompetitionservice.task;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xoj.xojbackendcompetitionservice.service.CompetitionService;
import com.yupi.yuojbackendmodel.model.entity.Competition;
import com.yupi.yuojbackendmodel.model.enums.CompetitionStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 竞赛状态定时刷新任务
 */
@Component
@Slf4j
public class CompetitionStatusTask {

    @Resource
    private CompetitionService competitionService;

    /**
     * 每分钟刷新一次竞赛状态。
     * 前提：startTime / endTime 统一存为 yyyy-MM-dd HH:mm:ss 格式，字符串可直接按字典序比较时间先后。
     */
    @Scheduled(cron = "0 * * * * ?")
    public void refreshCompetitionStatus() {
        String now = DateUtil.now();
        // 已结束：endTime 非空且已到
        competitionService.update(Wrappers.<Competition>lambdaUpdate()
                .set(Competition::getStatus, CompetitionStatusEnum.ENDED.getCode())
                .ne(Competition::getStatus, CompetitionStatusEnum.ENDED.getCode())
                .isNotNull(Competition::getEndTime)
                .ne(Competition::getEndTime, "")
                .le(Competition::getEndTime, now));
        // 进行中：startTime 已到且 endTime 未到
        competitionService.update(Wrappers.<Competition>lambdaUpdate()
                .set(Competition::getStatus, CompetitionStatusEnum.ONGOING.getCode())
                .ne(Competition::getStatus, CompetitionStatusEnum.ONGOING.getCode())
                .le(Competition::getStartTime, now)
                .and(wrapper -> wrapper.isNull(Competition::getEndTime)
                        .or().eq(Competition::getEndTime, "")
                        .or().gt(Competition::getEndTime, now)));
        // 待开始：startTime 未到
        competitionService.update(Wrappers.<Competition>lambdaUpdate()
                .set(Competition::getStatus, CompetitionStatusEnum.PENDING.getCode())
                .ne(Competition::getStatus, CompetitionStatusEnum.PENDING.getCode())
                .gt(Competition::getStartTime, now));
        log.info("refreshCompetitionStatus done, now = {}", now);
    }
}
