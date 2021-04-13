package com.dilatush.email;

/**
 * Encapsulates the notion of a result from an operation.  The "ok" flag should always be set to true if the result of the operation was ok, or
 * false if it was not.  The msg field is intended for use as an explanatory when the result was not ok, but it could be used even when the results
 * were ok.  The info field can be used either for the result of the operation when the result was ok, or some additional info if the result was not
 * ok.
 */
public record Result<T>( boolean ok, String msg, T info ) {


    /** Convenience instance of {@link Result} with OK results and no message or info */
    public static final Result<?> OK = new Result<>();


    /**
     * Convenience constructor that creates an instance of {@link Result} with the given ok status and message.
     *
     * @param ok  {@code true} if the new result is ok.
     * @param msg The message to be contained by the new result.
     */
    public Result( boolean ok, String msg ) {
        this( ok, msg, null );
    }


    /**
     * Convenience constructor that creates an instance of {@link Result} that is not ok, and has the given message and info.
     *
     * @param msg The message to be contained by the new result.
     * @param info The info to be contained by the new result.
     */
    public Result( String msg, T info ) {
        this( false, msg, info );
    }


    /**
     * Convenience constructor that creates an instance of {@link Result} that is not ok, has the given message, and has no info.
     *
     * @param msg The message to be contained by the new result.
     */
    public Result( String msg ) {
        this( false, msg, null );
    }


    /**
     * Convenience constructor that creates an instance of {@link Result} that is ok, has no message, and has no info.
     */
    public Result() {
        this( true, null, null );
    }
}
