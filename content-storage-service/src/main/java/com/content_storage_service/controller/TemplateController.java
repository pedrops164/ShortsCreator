package com.content_storage_service.controller;

import org.springframework.web.bind.annotation.*;

import com.content_storage_service.model.CharacterPreset;
import com.content_storage_service.repository.CharacterPresetRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/presets")
@RequiredArgsConstructor
@Slf4j
public class TemplateController {

    private final CharacterPresetRepository characterPresetRepository;
    
    @GetMapping("/characters")
    public Flux<CharacterPreset> getCharacterPresets() {
        log.info("Reactively fetching all character presets.");
        return characterPresetRepository.findAll();
    }
}
