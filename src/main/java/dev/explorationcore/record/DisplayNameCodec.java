package dev.explorationcore.record;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class DisplayNameCodec {
    private DisplayNameCodec() {
    }

    public static String decode(String token) {
        if (token == null || token.isBlank() || containsWhitespace(token)) {
            throw new IllegalArgumentException("display name token is blank");
        }
        if (token.indexOf('=') >= 0) {
            throw new IllegalArgumentException("display name token must be unpadded base64url");
        }
        int remainder = token.length() % 4;
        if (remainder == 1) {
            throw new IllegalArgumentException("display name token is not valid base64url");
        }
        String padded = switch (remainder) {
            case 2 -> token + "==";
            case 3 -> token + "=";
            default -> token;
        };
        byte[] decoded;
        try {
            decoded = Base64.getUrlDecoder().decode(padded);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("display name token is not valid base64url");
        }
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        String text;
        try {
            text = decoder.decode(ByteBuffer.wrap(decoded)).toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("display name token is not valid UTF-8");
        }
        if (text.isBlank()) {
            throw new IllegalArgumentException("display name is blank");
        }
        return text;
    }

    private static boolean containsWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
