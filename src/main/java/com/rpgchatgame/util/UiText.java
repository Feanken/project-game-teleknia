package com.rpgchatgame.util;

import java.util.*;
import java.util.regex.*;

/** English resource keys keep UI text separate from gameplay IDs and stored narration. */
public final class UiText {
    private static String language="en";
    private static final Map<String,String> ID=load("id");
    private static final Map<String,String> EN=load("en");
    private record Template(Pattern pattern,String value,List<String> slots) {}
    private static final List<Template> TEMPLATES=templates();
    private UiText() {}
    @SuppressWarnings("unchecked") public static Map<String,String> load(String code) {
        try(var input=UiText.class.getResourceAsStream("/i18n/ui-"+code+".json")) {
            if(input==null)throw new IllegalStateException("Missing UI language: "+code);
            return Map.copyOf(JsonUtil.fromJson(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8),Map.class));
        }catch(java.io.IOException error){throw new ExceptionInInitializerError(error);}
    }
    private static List<Template> templates(){
        List<Template> result=new ArrayList<>();
        List<String> keys=new ArrayList<>(ID.keySet());keys.sort(Comparator.comparingInt(String::length).reversed());
        for(String key:keys){String value=ID.get(key);
            Matcher matcher=Pattern.compile("\\{\\d+}").matcher(key);
            int end=0;StringBuilder regex=new StringBuilder("^");List<String> slots=new ArrayList<>();
            while(matcher.find()){regex.append(Pattern.quote(key.substring(end,matcher.start()))).append("(.*?)");slots.add(matcher.group());end=matcher.end();}
            if(!slots.isEmpty()){regex.append(Pattern.quote(key.substring(end))).append("$");result.add(new Template(Pattern.compile(regex.toString()),value,slots));}
        }
        return result;
    }
    public static void setLanguage(String value){if(!value.equals("en")&&!value.equals("id"))throw new IllegalArgumentException("Unsupported UI language");language=value;}
    public static String language(){return language;}
    public static String tr(String source){
        if(source==null||!language.equals("id"))return source;
        if(ID.containsKey(source))return ID.get(source);
        for(var template:TEMPLATES){var match=template.pattern().matcher(source);if(match.matches()){
            String value=template.value();for(int i=0;i<template.slots().size();i++)value=value.replace(template.slots().get(i),match.group(i+1));return value;
        }}
        return source;
    }
    public static String format(String key,Object... args){String value=tr(key);for(int i=0;i<args.length;i++)value=value.replace("{"+i+"}",String.valueOf(args[i]));return value;}
    public static boolean resourcesMatch(){return EN.keySet().equals(ID.keySet());}
}
