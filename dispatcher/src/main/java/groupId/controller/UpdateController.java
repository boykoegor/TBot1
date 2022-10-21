package groupId.controller;

import groupId.service.UpdateProducer;
import groupId.utils.MessageUtils;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import static groupId.model.RabbitQueue.*;

@Component
@Log4j
public class UpdateController {
    private TelegramBot telegramBot;
    private final MessageUtils messageUtils;
    private final UpdateProducer updateProducer;

    public UpdateController(MessageUtils messageUtils, UpdateProducer updateProducer) {
        this.messageUtils = messageUtils;
        this.updateProducer = updateProducer;
    }

    public void registerBot(TelegramBot telegramBot){
        this.telegramBot = telegramBot;
    }

    public void procesUpdate(Update update){
        if (update == null){
            log.error("Received update is null");
            return;
        }

        if(update.getMessage() != null){
            distributeMessageByType(update);
        } else {
            log.error("Unsupported message typ is received: "+update);
        }
    }

    private void distributeMessageByType(Update update) {
        var message = update.getMessage();
        if (message.getText() != null){
            processTextMessage(update);
        } else if (message.getDocument() != null){
            processDocumentMessage(update);
        } else if (message.getPhoto() != null){
            processPhotoMessage(update);
        } else {
            setUnsupportedMessageTypeView(update);
        }
    }

    private void setUnsupportedMessageTypeView(Update update) {
        var sendMessage = messageUtils.generateSendMessageWithText(update,
                "Неподдерживаемый формат файла");
        setView(sendMessage);
    }

    private void setView(SendMessage sendMessage) {
        telegramBot.sendAnswerMessage(sendMessage);
    }

    private void setFileIsReceivedView(Update update) {
        var sendMessage = messageUtils.generateSendMessageWithText(update,
                "Файл получен. Обробатывается...");
        setView(sendMessage);
    }

    private void processPhotoMessage(Update update) {
        updateProducer.produce(PHOTO_MESSAGE_UPDATE, update);
        setFileIsReceivedView(update);
    }

    private void processDocumentMessage(Update update) {
        updateProducer.produce(DOC_MESSAGE_UPDATE, update);

    }

    private void processTextMessage(Update update) {
        updateProducer.produce(TEXT_MESSAGE_UPDATE, update);

    }
}
