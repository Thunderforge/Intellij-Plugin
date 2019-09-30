// Copyright 2015 ThoughtWorks, Inc.

// This file is part of getgauge/Intellij-plugin.

// getgauge/Intellij-plugin is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// getgauge/Intellij-plugin is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with getgauge/Intellij-plugin.  If not, see <http://www.gnu.org/licenses/>.

package com.thoughtworks.gauge.formatter;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.thoughtworks.gauge.Constants;
import com.thoughtworks.gauge.settings.GaugeSettingsModel;
import com.thoughtworks.gauge.util.GaugeUtil;

import java.io.File;

import static com.thoughtworks.gauge.util.GaugeUtil.getGaugeSettings;

public class SpecFormatter extends AnAction {
    private static final Logger LOG = Logger.getInstance("#com.thoughtworks.gauge.formatter.SpecFormatter");
    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getData(LangDataKeys.PROJECT);
        if (project == null) {
            return;
        }
        String projectDir = project.getBasePath();
        if (projectDir == null) {
            return;
        }

        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile selectedFile = fileEditorManager.getSelectedFiles()[0];
        String fileName = selectedFile.getCanonicalPath();
        Document doc = FileDocumentManager.getInstance().getDocument(selectedFile);
        if (doc != null) {
            FileDocumentManager.getInstance().saveDocument(doc);
        }
        try {
            GaugeSettingsModel settings = getGaugeSettings();
            ProcessBuilder processBuilder = new ProcessBuilder(settings.getGaugePath(), Constants.FORMAT, fileName);
            GaugeUtil.setGaugeEnvironmentsTo(processBuilder, settings);
            processBuilder.directory(new File(projectDir));
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String output = String.format("<pre>%s</pre>", GaugeUtil.getOutput(process.getInputStream(), "\n").replace("<", "&lt;").replace(">", "&gt;"));
                Notifications.Bus.notify(new Notification("Spec Formatting", "Error: Spec Formatting", output, NotificationType.ERROR));
                return;
            }
            VirtualFileManager.getInstance().syncRefresh();
            selectedFile.refresh(false, false);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.debug(e);
            Messages.showErrorDialog("Error on formatting spec", "Format Error");
        }
    }
}
