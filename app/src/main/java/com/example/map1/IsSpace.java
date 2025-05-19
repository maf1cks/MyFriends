package com.example.map1;

public class IsSpace {
    public static boolean isSpase(final String str){
        if(str != null){
            for(int i = 0; i < str.length(); i++){
                if (Character.isWhitespace(str.charAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }
    public static boolean check(final String str){
        String[] mas = {String.valueOf('@'), String.valueOf('#'), String.valueOf('$'), String.valueOf('%'), String.valueOf('^'), String.valueOf('&'), String.valueOf('*'), String.valueOf('('), String.valueOf(')'), String.valueOf('-'), String.valueOf('+'), String.valueOf('='), String.valueOf('{'), String.valueOf('}'), String.valueOf('['),String.valueOf('!'), String.valueOf(']'), String.valueOf(':'), String.valueOf(';'), String.valueOf('"'), String.valueOf('<'), String.valueOf('>'), String.valueOf(','), String.valueOf('.'), String.valueOf('?'), String.valueOf('/'), String.valueOf('|')};
        for (String i : mas){
            if (str.contains(i)){
                return true;
            }
        }
        return false;
    }
}
