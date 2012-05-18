/*
 * The MIT License
 *
 * Copyright (c) 2011 eXo platform
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkins.plugins.glanceupload;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Result;
import hudson.plugins.promoted_builds.Promotion;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.RunList;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GlanceUploadPublisher extends Recorder {

    private String identity;
    public String getIdentity() {
        return this.identity;
    }
    private String credentials;
    public String getCredentials() {
        return this.credentials;
    }
    private String serverURL;
    public String getServerURL() {
        return this.serverURL;
    }
    private String artifact;
    public String getArtifact() {
        return this.artifact;
    }
    
    @DataBoundConstructor
    public GlanceUploadPublisher(String identity, String credentials, String serverURL, String artifact) {
        this.identity = identity;
        this.credentials = credentials;
        this.serverURL = serverURL;
        this.artifact = artifact;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("Server URL " + getServerURL() + ", artifact " + getArtifact());

        if (build.getResult().isWorseOrEqualTo(Result.FAILURE))
            return true; // nothing to do

        // Validates the necessaries
        if (StringUtils.isBlank(getIdentity()) || StringUtils.isBlank(getCredentials()) 
            || StringUtils.isBlank(getServerURL()) || StringUtils.isBlank(getArtifact())) {
            listener.error("Please fill out all the necessaries!");
            return false;
        }

        //Get the artifact
        FileFinder fileFinder = new FileFinder(getArtifact());

        FilePath rootDir;
        rootDir = build.getWorkspace();
        if (rootDir==null) { // slave down?
            listener.error("Cannot find the workspace");
            return false;
        }

        listener.getLogger().println("Root dir is " + rootDir);
    
        List<String> fileNames = rootDir.act(fileFinder);
        listener.getLogger().println("Files to upload " + fileNames);
    
        if (fileNames.isEmpty()) {
            listener.error("No artifacts found " + getArtifact());
            return false;
        }
    
        boolean result=true;
        
        // One day init glance and upload
        
        return result;
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        public DescriptorImpl() {
            super(GlanceUploadPublisher.class);
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            // XXX is this now the right style?
            req.bindJSON(this, json);
            save();
            return true;
        }

        /**
         * Performs on-the-fly validation on the file mask wildcard.
         */
        public FormValidation doCheckFilePattern(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            return FilePath.validateFileMask(project.getSomeWorkspace(),value);
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Upload to Glance";
        }
    }
}
