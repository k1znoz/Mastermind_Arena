package io.mastermindarena.deduction.engine.workflow;

/** Expected game-rule refusal. Its stable code is safe to publish; technical diagnostics are not. */
public final class ActionRejectedException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    public ActionRejectedException(String code) {
        super(code);
    }

    public ActionRejectedException(String code, Throwable cause) {
        super(code, cause);
    }
}
