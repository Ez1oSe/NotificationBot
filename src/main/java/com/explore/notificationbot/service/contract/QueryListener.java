package com.explore.notificationbot.service.contract;

import com.explore.notificationbot.bot.Bot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface QueryListener {
    BotApiMethod<?> answerQuery(CallbackQuery query, String[] words, Bot bot) throws TelegramApiException;
}
