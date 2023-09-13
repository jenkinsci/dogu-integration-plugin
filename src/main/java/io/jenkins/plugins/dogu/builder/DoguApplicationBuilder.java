package io.jenkins.plugins.dogu.builder;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import io.jenkins.plugins.dogu.DoguOption;
import io.jenkins.plugins.dogu.common.DoguApi;
import io.jenkins.plugins.dogu.common.DoguUtils;
import io.jenkins.plugins.dogu.credential.DoguCredential;
import java.io.PrintStream;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

public class DoguApplicationBuilder extends Builder {
    private final String projectId;
    private final String applicationPath;
    private final Boolean isLatest;
    private final String credentialsId;

    @DataBoundConstructor
    public DoguApplicationBuilder(String projectId, String applicationPath, String credentialsId, Boolean isLatest) {
        System.out.println("Builder: " + isLatest);
        this.projectId = projectId;
        this.applicationPath = applicationPath;
        this.credentialsId = credentialsId;
        this.isLatest = isLatest;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getApplicationPath() {
        return applicationPath;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public Boolean getIsLatest() {
        return isLatest;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        PrintStream logger = listener.getLogger();

        Secret accessTokenSecret = DoguUtils.getAccessToken(credentialsId);
        String apiUrl = DoguUtils.getApiUrl(build, listener, logger);
        DoguOption doguOption = new DoguOption(accessTokenSecret, apiUrl);

        try {
            DoguApi.uploadApplication(applicationPath, projectId, isLatest, doguOption, logger);
        } catch (Exception e) {
            e.printStackTrace(logger);
            return false;
        }

        return true;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Upload application to Dogu";
        }

        @RequirePOST
        public FormValidation doCheckCredentialsId(@QueryParameter String value) {
            try {
                Jenkins.get().checkPermission(Jenkins.READ);
            } catch (Exception e) {
                return FormValidation.error("Please login");
            }

            if (value.isEmpty()) {
                return FormValidation.error("Please select credentials");
            }

            return FormValidation.ok();
        }

        @RequirePOST
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (item == null || !item.hasPermission(Item.CONFIGURE)) {
                return result.includeCurrentValue(credentialsId);
            }

            return result.includeEmptyValue()
                    .includeAs(ACL.SYSTEM, item, DoguCredential.class)
                    .includeCurrentValue(credentialsId);
        }
    }
}
