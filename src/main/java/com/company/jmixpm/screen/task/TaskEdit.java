package com.company.jmixpm.screen.task;

import com.company.jmixpm.app.TaskService;
import com.company.jmixpm.entity.Task;
import io.jmix.core.FileRef;
import io.jmix.ui.component.BrowserFrame;
import io.jmix.ui.component.FileStorageResource;
import io.jmix.ui.model.InstanceContainer;
import io.jmix.ui.screen.*;
import org.springframework.beans.factory.annotation.Autowired;

@UiController("Task_.edit")
@UiDescriptor("task-edit.xml")
@EditedEntityContainer("taskDc")
public class TaskEdit extends StandardEditor<Task> {
    @Autowired
    private TaskService taskService;

    @Autowired
    private BrowserFrame browserFrame;

    @Subscribe
    public void onInitEntity(InitEntityEvent<Task> event) {
        event.getEntity().setAssignee(taskService.findLeastBusyUser());
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        refreshBrowserFrame();
    }

    @Subscribe(id = "taskDc", target = Target.DATA_CONTAINER)
    public void onTaskDcItemPropertyChange(InstanceContainer.ItemPropertyChangeEvent<Task> event) {
        if ("attachment".equals(event.getProperty())) {
            refreshBrowserFrame();
        }
    }

    private void refreshBrowserFrame() {
        FileRef attachment = getEditedEntity().getAttachment();
        if (attachment != null) {
            browserFrame.setSource(FileStorageResource.class).setFileReference(attachment)
                    .setMimeType(attachment.getContentType());
        }
    }
}