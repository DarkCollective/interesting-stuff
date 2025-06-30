package com.darkcollective.wordverifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class WordVerificationController {

    @Autowired
    private WordVerificationService wordVerificationService;

    @PostMapping(value = "/verify", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String verifyWords(@RequestBody String input) {
        return wordVerificationService.verifyWords(input);
    }
}