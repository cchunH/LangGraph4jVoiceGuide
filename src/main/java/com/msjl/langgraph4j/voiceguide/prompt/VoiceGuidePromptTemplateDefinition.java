package com.msjl.langgraph4j.voiceguide.prompt;

import java.util.ArrayList;
import java.util.List;

public class VoiceGuidePromptTemplateDefinition {

    private String focus;
    private SystemPromptDefinition system = new SystemPromptDefinition();
    private UserPromptDefinition user = new UserPromptDefinition();

    public String getFocus() {
        return focus;
    }

    public void setFocus(String focus) {
        this.focus = focus;
    }

    public SystemPromptDefinition getSystem() {
        return system;
    }

    public void setSystem(SystemPromptDefinition system) {
        this.system = system;
    }

    public UserPromptDefinition getUser() {
        return user;
    }

    public void setUser(UserPromptDefinition user) {
        this.user = user;
    }

    public static class SystemPromptDefinition {

        private String role;
        private String objective;
        private List<String> rules = new ArrayList<>();
        private List<String> constraints = new ArrayList<>();
        private List<String> workflow = new ArrayList<>();
        private List<String> outputContract = new ArrayList<>();

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getObjective() {
            return objective;
        }

        public void setObjective(String objective) {
            this.objective = objective;
        }

        public List<String> getRules() {
            return rules;
        }

        public void setRules(List<String> rules) {
            this.rules = rules;
        }

        public List<String> getConstraints() {
            return constraints;
        }

        public void setConstraints(List<String> constraints) {
            this.constraints = constraints;
        }

        public List<String> getWorkflow() {
            return workflow;
        }

        public void setWorkflow(List<String> workflow) {
            this.workflow = workflow;
        }

        public List<String> getOutputContract() {
            return outputContract;
        }

        public void setOutputContract(List<String> outputContract) {
            this.outputContract = outputContract;
        }
    }

    public static class UserPromptDefinition {

        private String sectionName;
        private List<String> inputChecklist = new ArrayList<>();
        private List<String> rewriteGoals = new ArrayList<>();
        private List<String> outputRequirements = new ArrayList<>();
        private String template;
        private String example;

        public String getSectionName() {
            return sectionName;
        }

        public void setSectionName(String sectionName) {
            this.sectionName = sectionName;
        }

        public List<String> getInputChecklist() {
            return inputChecklist;
        }

        public void setInputChecklist(List<String> inputChecklist) {
            this.inputChecklist = inputChecklist;
        }

        public List<String> getRewriteGoals() {
            return rewriteGoals;
        }

        public void setRewriteGoals(List<String> rewriteGoals) {
            this.rewriteGoals = rewriteGoals;
        }

        public List<String> getOutputRequirements() {
            return outputRequirements;
        }

        public void setOutputRequirements(List<String> outputRequirements) {
            this.outputRequirements = outputRequirements;
        }

        public String getTemplate() {
            return template;
        }

        public void setTemplate(String template) {
            this.template = template;
        }

        public String getExample() {
            return example;
        }

        public void setExample(String example) {
            this.example = example;
        }
    }
}
