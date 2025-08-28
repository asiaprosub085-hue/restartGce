package com.example.gcerestarter.bot;

import com.example.gcerestarter.model.GceRequest;
import com.example.gcerestarter.model.GceResponse;
import com.example.gcerestarter.service.GceService;
import com.example.gcerestarter.utils.ByteUnitConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CustomTemplateBot extends TelegramLongPollingBot {
    // 替换为你的机器人Token和用户名
    private static final String BOT_TOKEN = "8432908571:AAGLoVD7LNglAGHJhXOFk9wRSI0dh-4_OWE";
    private static final String BOT_USERNAME = "yazhou_gaojing_bot";
    // 用于分割参数的分隔符（避免与参数内容冲突）
    private static final String PARAM_DELIMITER = "|";
    private static final Logger logger = LoggerFactory.getLogger(CustomTemplateBot.class);
    // 1. 注入Service（构造函数注入更安全）
    private final GceService gceService;

    // 2. 构造函数注入（Spring保证注入完成后才创建实例）
    @Autowired
    public CustomTemplateBot(GceService gceService) {
        this.gceService = gceService;
    }

    // 静态实例，供外部直接调用
    private static CustomTemplateBot instance;

    // 初始化时赋值静态实例
    @PostConstruct
    public void init() {
        instance = this;
        // 这里可以安全访问GceService
        if (gceService == null) {
            throw new RuntimeException("GceService注入失败");
        }
    }

    public static CustomTemplateBot getInstance() {
        return instance;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // 1. 处理用户发送的消息（如/start命令）
        logger.error("点击重启回调 ： " + update.hasCallbackQuery());
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            if (update.getMessage().getText().equals("/start")) {
                try {
                    sendResponse(chatId, chatId);
                } catch (TelegramApiException e) {
                    System.err.println("发送消息失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        // 2. 处理按钮回调事件（用户点击按钮时触发）
        else if (update.hasCallbackQuery()) {
            // 获取回调数据和相关信息
            String callbackData = update.getCallbackQuery().getData();
            String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            try {
                // 解析回调数据中的参数
                processCallbackData(chatId, messageId, callbackData);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 解析回调数据并分发到对应方法
     */
    private void processCallbackData(String chatId, Integer messageId, String callbackData) throws TelegramApiException {
        // 分割回调数据（按自定义分隔符）
        String[] parts = callbackData.split("\\" + PARAM_DELIMITER);
        if (parts.length == 0) {
            sendResponse(chatId, "无效的操作");
            return;
        }

        // 第一个部分作为动作标识
        String action = parts[0];

        // 根据动作标识分发处理
        switch (action) {
            case "restart":
                // 验证参数数量
                if (parts.length >= 4) {
                    //点击后删除按钮
                    handleRemoveButtonAfterClick(chatId, messageId);

                    String resourceName = parts[1];
                    String zone = parts[2];
                    String projectId = parts[3];

                    GceRequest request = new GceRequest();
                    request.setInstanceName(resourceName);
                    request.setZone(zone);
                    request.setProjectId(projectId);
                    logger.error("request ： " + request.toString());
                    logger.error("gceService ： " + gceService);
                    try {
                        GceResponse response = gceService.restartInstance(request);
                        sendResponse(chatId, "实例: " + resourceName + " , 重启完成: " + response.getMessage());
                    } catch (Exception e) {
                        sendResponse(chatId, "实例: " + resourceName + " , 重启异常: " + e.getMessage());
                    }
                } else {
                    sendResponse(chatId, "商品参数不完整");
                }
                break;
            default:
                sendResponse(chatId, "未知操作: " + action);
        }
    }

    private void handleRemoveButtonAfterClick(String chatId, Integer messageId) throws TelegramApiException {
        // 移除按钮（设置空键盘）
        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
        editMarkup.setChatId(chatId);
        editMarkup.setMessageId(messageId);
        editMarkup.setReplyMarkup(null); // 设置为空表示移除键盘
        execute(editMarkup);
    }

    // 发送新消息
    private void sendResponse(String chatId, String text) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        execute(message);
    }

    public static void sendNoticMessage(Map<String, String> map) throws TelegramApiException {
        // 1. 创建消息对象
        SendMessage message = new SendMessage();
        message.setChatId(map.get("chatId"));
        // 2. 定义消息模板（支持HTML格式化）
        String template = "♨️♨️ %s \n" +
                " 实例名: <b>%s</b> \n" +
                " 异常值: %s \n" +
                " 限制值: %s \n" +
                " 异常时间:：<i>%s</i>\n\n";

        // 3. 替换模板变量
        String messageText = String.format(template,
                map.get("policyName"),
                map.get("resourceName"),
                replaceValue(map.get("policyName"), map.get("observedValue")),
                replaceValue(map.get("policyName"), map.get("thresholdValue")),
                map.get("startedAt"));
        message.setText(messageText);

        // 4. 设置格式化方式（HTML）
        message.setParseMode("HTML");

        // 5. 添加自定义按钮（可选）
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // 按钮1
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("查看详情");
        button1.setUrl(map.get("url"));
        // 按钮2
//        InlineKeyboardButton button2 = new InlineKeyboardButton();
//        button2.setText("重启实例");
//        button2.setCallbackData("restart"
//                + "|" + map.get("resourceName")
//                + "|" + map.get("zone")
//                + "|" + map.get("projectId"));

        // 添加按钮到行
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button1);
//        row.add(button2);
        rows.add(row);

        keyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            // 6. 发送消息
            instance.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException("消息发送失败: " + e.getMessage());
        }
    }

    public static String replaceValue(String name, String value) {
        if (name.contains("cpu异常告警")) {
            String values = new BigDecimal(value).multiply(new BigDecimal(100)) + "%";
            return String.format("%." + 2 + "f MB", values);
        } else if (name.contains("流量异常告警")) {
            return ByteUnitConverter.convertBytes(new BigDecimal(value).longValue(), 2);
        }
        return value;
    }
}
