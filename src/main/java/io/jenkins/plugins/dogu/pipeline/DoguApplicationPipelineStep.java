package io.jenkins.plugins.dogu.pipeline;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

public class DoguApplicationPipelineStep extends Step {
    private String applicationPath;
    private String projectId;
    private Boolean isLatest;
    private String credentialsId;

    @DataBoundConstructor
    public DoguApplicationPipelineStep(
            String applicationPath, String projectId, String credentialsId, Boolean isLatest) {
        this.applicationPath = applicationPath;
        this.projectId = projectId;
        this.credentialsId = credentialsId;
        this.isLatest = isLatest;
    }

    public String getApplicationPath() {
        return applicationPath;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public Boolean getIsLatest() {
        return isLatest;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new DoguApplicationPipelineStepExecution(this, context);
    }

    @Extension
    public static class StepDescriptorImpl extends StepDescriptor {
        public StepDescriptorImpl() {
            super();
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class, Launcher.class);
        }

        @Override
        public String getFunctionName() {
            return "doguUploadApplication";
        }

        @Override
        public String getDisplayName() {
            return "Dogu application step";
        }
    }
}
