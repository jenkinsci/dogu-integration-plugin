package io.jenkins.plugins.dogu.pipeline;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;
import io.jenkins.plugins.dogu.DoguOption;
import io.jenkins.plugins.dogu.common.DoguApi;
import io.jenkins.plugins.dogu.common.DoguUtils;
import java.io.PrintStream;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

public class DoguApplicationPipelineStepExecution extends StepExecution {
    private static final long serialVersionUID = 1L;
    private transient DoguApplicationPipelineStep step;

    @DataBoundConstructor
    public DoguApplicationPipelineStepExecution(DoguApplicationPipelineStep step, StepContext context) {
        super(context);
        this.step = step;
    }

    @Override
    public boolean start() throws Exception {
        String applicationPath = step.getApplicationPath();
        String projectId = step.getProjectId();
        Boolean isLatest = step.getIsLatest();
        String credentialsId = step.getCredentialsId();
        
        System.out.println("applicationPath: " + applicationPath);
        System.out.println("projectId: " + projectId);
        System.out.println("isLatest: " + isLatest.toString());

        Run<?, ?> build = getContext().get(Run.class);
        TaskListener listener = getContext().get(TaskListener.class);
        PrintStream logger = listener.getLogger();
        Secret accessTokenSecret = DoguUtils.getAccessToken(credentialsId);

        String apiUrl = DoguUtils.getApiUrl(build, listener, logger);
        DoguOption doguOption = new DoguOption(accessTokenSecret, apiUrl);

        try {
            boolean uploadResult = DoguApi.uploadApplication(applicationPath, projectId, isLatest, doguOption, logger);
            getContext().onSuccess("Success");
            return uploadResult;
        } catch (Exception e) {
            e.printStackTrace(logger);
            getContext().onFailure(e);
            return false;
        }
    }

    @Override
    public void stop(@Nonnull Throwable cause) throws Exception {}
}
