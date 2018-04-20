package mrl.world.routing;

/**
 * User: roohola
 * Date: 3/31/11
 * Time: 3:50 PM
 */
public class NoRouteFoundException extends RuntimeException {
    public NoRouteFoundException() {
    }

    public NoRouteFoundException(String message) {
        super(message);
    }
}
