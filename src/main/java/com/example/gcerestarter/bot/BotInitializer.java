package com.example.gcerestarter.bot;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.annotation.Resource;

// 仅当不使用spring-boot-starter时需要此类
@Component
public class BotInitializer {

    // 注入你的机器人实例（已被Spring管理）
    @Resource
    private CustomTemplateBot telegramSender;

    // 监听Spring启动完成事件（确保所有Bean已初始化）
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            // 手动创建TelegramBotsApi并注册机器人
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramSender);
            System.out.println("机器人手动注册成功");
        } catch (TelegramApiException e) {
            throw new RuntimeException("机器人注册失败: " + e.getMessage());
        }
    }
}
