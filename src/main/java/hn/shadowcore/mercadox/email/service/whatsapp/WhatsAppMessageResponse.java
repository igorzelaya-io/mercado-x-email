package hn.shadowcore.mercadox.email.service.whatsapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppMessageResponse(List<Message> messages) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String id) {}
}
