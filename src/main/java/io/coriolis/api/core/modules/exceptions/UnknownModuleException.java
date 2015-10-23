package io.coriolis.api.core.modules.exceptions;

/**
 * Created by cmmcleod on 10/16/15.
 */
public class UnknownModuleException extends Exception {

    public UnknownModuleException(String name, String clazz, String rating, String mount,String guidance, String ship) {
        super(name + " " + clazz + rating + (mount != null ? "/" + mount + guidance : "") + (ship != null ? " for ship " + ship : ""));
    }
}
