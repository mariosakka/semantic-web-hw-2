package com.example.semanticweb.controller;

import com.example.semanticweb.service.ChatbotService;
import com.example.semanticweb.service.GraphRenderService;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/rdf")
public class RdfController {

    private final GraphRenderService graphRenderService;
    private final ChatbotService chatbotService;

    public RdfController(GraphRenderService graphRenderService, ChatbotService chatbotService) {
        this.graphRenderService = graphRenderService;
        this.chatbotService = chatbotService;
    }

    @GetMapping("/upload")
    public String uploadPage(ModelMap model) {
        addChat(model);
        return "rdf-upload";
    }

    @PostMapping("/upload")
    public String uploadRdf(@RequestParam("rdfFile") MultipartFile rdfFile, ModelMap model) {
        if (rdfFile == null || rdfFile.isEmpty()) {
            model.addAttribute("error", "Please choose an RDF file.");
            addChat(model);
            return "rdf-upload";
        }

        try {
            Model rdfModel = ModelFactory.createDefaultModel();
            rdfModel.read(rdfFile.getInputStream(), null, "RDF/XML");
            String graphSvg = graphRenderService.renderModelToSvg(rdfModel);

            model.addAttribute("graphSvg", graphSvg);
            model.addAttribute("fileName", rdfFile.getOriginalFilename());
            addChat(model);
            return "rdf-graph";
        } catch (Exception e) {
            model.addAttribute("error", "Could not parse or render RDF file.");
            addChat(model);
            return "rdf-upload";
        }
    }

    private void addChat(ModelMap model) {
        model.addAttribute("chatStarters", chatbotService.startersForGeneralPage());
        model.addAttribute("chatRedirectPath", "/rdf/upload");
        model.addAttribute("chatBookId", "");
    }
}
