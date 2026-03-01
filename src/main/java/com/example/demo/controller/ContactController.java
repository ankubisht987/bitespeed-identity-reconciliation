package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.demo.service.ContactService;
import com.example.demo.dto.IdentifyRequest;
import com.example.demo.dto.IdentifyResponse;

@RestController
@RequestMapping("/identify")
public class ContactController {

    @Autowired
    private ContactService contactService;

    @PostMapping
    public IdentifyResponse identify(@RequestBody IdentifyRequest request) {
        return contactService.identify(request);
    }
    
}
