package com.company.jmixpm.screen.task;

import com.company.jmixpm.entity.Project;
import com.company.jmixpm.entity.Task;
import com.company.jmixpm.entity.User;
import io.jmix.core.DataManager;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.ui.UiComponents;
import io.jmix.ui.component.*;
import io.jmix.ui.download.Downloader;
import io.jmix.ui.screen.*;
import io.jmix.ui.screen.LookupComponent;
import io.jmix.ui.upload.TemporaryStorage;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@UiController("Task_.browse")
@UiDescriptor("task-browse.xml")
@LookupComponent("tasksTable")
public class TaskBrowse extends StandardLookup<Task> {
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private Downloader downloader;
    @Autowired
    private TemporaryStorage temporaryStorage;
    @Autowired
    private FileStorageUploadField tasksUpload;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Install(to = "tasksTable.attachment", subject = "columnGenerator")
    private Component tasksTableAttachmentColumnGenerator(Task task) {
        if (task.getAttachment() == null) {
            return new Table.PlainTextCell("");
        } else {
            LinkButton linkButton = uiComponents.create(LinkButton.class);
            linkButton.setCaption(task.getAttachment().getFileName());
            linkButton.addClickListener(clickEvent -> {
                downloader.download(task.getAttachment());
            });
            return linkButton;
        }
    }

    @Subscribe("tasksUpload")
    public void onTasksUploadFileUploadSucceed(SingleFileUploadField.FileUploadSucceedEvent event) throws IOException {
        UUID fileId = tasksUpload.getFileId();
        if (fileId == null) {
            return;
        }

        File file = temporaryStorage.getFile(fileId);

        List<Task> tasks = processFileWithTasks(file);

        dataManager.save(tasks.toArray());

        temporaryStorage.deleteFile(fileId);

        getScreenData().loadAll();
    }

    private List<Task> processFileWithTasks(File file) throws IOException {
        List<String> tasksNames = FileUtils.readLines(file, StandardCharsets.UTF_8);
        List<Task> tasks = new ArrayList<>(tasksNames.size());

        Project defaultProject = loadDefaultProject();
        if (defaultProject == null) {
            return Collections.emptyList();
        }

        for (String taskName : tasksNames) {
            Task task = dataManager.create(Task.class);
            task.setName(taskName);
            task.setProject(defaultProject);
            task.setAssignee((User) currentAuthentication.getUser());
            tasks.add(task);
        }
        return tasks;
    }

    @Nullable
    private Project loadDefaultProject() {
        return dataManager.load(Project.class)
                .query("select p from Project p where p.defaultProject = :defaultProject")
                .parameter("defaultProject", true)
                .optional().orElse(null);
    }
}