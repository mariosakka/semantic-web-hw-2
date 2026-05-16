package com.example.semanticweb.controller;

import com.example.semanticweb.service.ChatbotService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ChatbotService chatbotService;

    public HomeController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @GetMapping("/")
    public String index(Model model) {
        addChat(model, "/");
        return "index";
    }

    private void addChat(Model model, String redirectPath) {
        model.addAttribute("chatStarters", chatbotService.startersForGeneralPage());
        model.addAttribute("chatRedirectPath", redirectPath);
        model.addAttribute("chatBookId", "");
    }
}
