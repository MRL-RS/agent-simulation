package mrl.common;

/**
 * User: mrl
 * Date: 6/13/12
 * Time: 6:00 PM
 */
public class IncorrectInputException extends RuntimeException {
    public IncorrectInputException(String message) {
        System.err.println(message);
    }
}
