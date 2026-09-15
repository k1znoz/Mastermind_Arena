package io.mastermindarena.deduction.api.submitaction;

import io.mastermindarena.deduction.application.submitaction.SubmitActionApplicationResponse;
import io.mastermindarena.deduction.application.submitaction.SubmitActionApplicationService;

import java.util.Objects;

public final class LocalSubmitActionEndpoint {
    public static final String PATH = "/local/submit-action";
    public static final String METHOD = "POST";

    private final SubmitActionApplicationService applicationService;

    public LocalSubmitActionEndpoint(SubmitActionApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService, "applicationService is required");
    }

    public SubmitActionHttpResponse.Envelope postSubmitAction(SubmitActionHttpRequest request) {
        SubmitActionApplicationResponse applicationResponse = applicationService.submit(
                SubmitActionHttpMapper.toApplicationRequest(request)
        );
        return SubmitActionHttpMapper.fromApplicationResponse(applicationResponse);
    }
}
