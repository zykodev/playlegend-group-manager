package net.playlegend.groupmanager.tasks;

import net.playlegend.groupmanager.GroupManager;

public class TaskRebuild implements Runnable {

  @Override
  public void run() {
    GroupManager.getInstance().rebuildEverything();
  }
}
