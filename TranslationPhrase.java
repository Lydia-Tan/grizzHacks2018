package com.example.karl.grizzhacksproject;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translate.TranslateOption;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

public class TranslatePhrase{
    private String phrase;
    private String currentLang;
    private String newLang;

    public TranslatePhrase(String p, String c, String n) {
        this.phrase = p;
        this.currentLang = c;
        this.newLang = n;
    }
    public String translateText(String phrase, String currentLang, String newLang){
        // Instantiates a client
        Translate translate = TranslateOptions.getDefaultInstance().getService();

        Translation translation = translate.translate(phrase, TranslateOption.sourceLanguage(currentLang),
                TranslateOption.targetLanguage(newLang));

        return("Translation: %s%n", translation.getTranslatedText());
    }

    public String getPhrase(){
        return ("Original: %s%n", phrase);
    }

    public String getCurrentLang(){
        return ("Current Language: %s%n", currentLang);
    }

    public String getNewLang(){
        return ("Intended Translated Language: %s%n", newLang);
    }
}
