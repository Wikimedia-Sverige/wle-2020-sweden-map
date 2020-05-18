package se.wikimedia.service.template.prevayler;

/**
 * @author kalle
 * @since 2019-05-27
 */
public interface Command<R, E extends Exception> {

  public R execute() throws E;

}
