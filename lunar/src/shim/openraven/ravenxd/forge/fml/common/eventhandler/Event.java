package openraven.ravenxd.forge.fml.common.eventhandler;
/** Minimal stand-in for Forge's Event. Every event is cancelable (lenient). */
public class Event {
    public enum Result { DENY, DEFAULT, ALLOW }
    private boolean canceled; private Result result = Result.DEFAULT;
    public Event() {}
    public boolean isCancelable() { return true; }
    public boolean isCanceled() { return canceled; }
    public void setCanceled(boolean cancel) { canceled = cancel; }
    public boolean hasResult() { return false; }
    public Result getResult() { return result; }
    public void setResult(Result value) { result = value; }
}
