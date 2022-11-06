package net.playlegend.groupmanager.tasks;

import net.playlegend.groupmanager.GroupManagerPlugin;

public class TaskRebuild implements Runnable {

  @Override
  public void run() {
    GroupManagerPlugin.getInstance().rebuildEverything();
  }
}
